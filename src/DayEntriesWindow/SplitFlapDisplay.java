package DayEntriesWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Eine benutzerdefinierte Swing-Komponente (Custom Component), die eine
 * mechanische Fallblattanzeige (Split-Flap Display) simuliert.
 *
 * <p>Lernziele in dieser Klasse:</p>
 * <ul>
 * <li><b>JComponent erweitern:</b> Wie man eigene GUI-Elemente baut.</li>
 * <li><b>Custom Painting (paintComponent):</b> Wie man Pixel für Pixel selbst zeichnet.</li>
 * <li><b>Animation (javax.swing.Timer):</b> Wie man Bewegung in Swing-GUIs bringt, ohne den Main-Thread zu blockieren.</li>
 * <li><b>Zustandsverwaltung:</b> Unterschied zwischen dem, was man sieht (current), und dem Ziel (target).</li>
 * </ul>
 */
public class SplitFlapDisplay extends JComponent {

    // Der Zeichensatz, den die Anzeige darstellen kann.
    // Das ist wie das physische Rad in der Anzeige: Es kann sich nur durch diese Zeichen drehen.
    private static final String CHARSET = " ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜß0123456789.,:;!?-+*/()[]\"'";

    // Konfiguration der Dimensionen (Zeilen x Spalten)
    private final int rows;
    private final int cols;

    // ===== ZUSTANDS-ARRAYS =====
    // Hier liegt der Kern der Animation:
    // 'current': Das Zeichen, das gerade AUF DEM BILDSCHIRM sichtbar ist.
    // 'target': Das Zeichen, das wir am Ende sehen WOLLEN.
    // Der Timer sorgt dafür, dass sich 'current' schrittweise 'target' annähert.
    private final char[][] current;
    private final char[][] target;

    // Der Swing-Timer für die Animation.
    // Swing ist "Single Threaded", daher nutzen wir javax.swing.Timer (nicht java.util.Timer),
    // damit die UI-Updates sicher auf dem Event-Dispatch-Thread (EDT) laufen.
    private Timer timer;

    // Zähler für die Animationsschritte (Frames)
    private int tick = 0;

    // ===== OPTIK & LAYOUT KONFIGURATION =====
    private int cellW = 22; // Breite eines Zeichens in Pixel
    private int cellH = 30; // Höhe eines Zeichens
    private int gapX = 3;   // Horizontaler Abstand zwischen Zeichen
    private int gapY = 6;   // Vertikaler Abstand zwischen Zeilen

    // Timing-Einstellungen
    private int ticksPerChar = 9;      // Wie schnell "blättert" ein einzelnes Zeichen?
    private int rowStaggerTicks = 6;   // Verzögerung, bis die nächste Zeile anfängt (Domino-Effekt)
    private int maxAdvancesPerTick = 6; // Wie viele Buchstaben überspringen wir pro Frame? (Geschwindigkeit)

    private Font flapFont = new Font(Font.MONOSPACED, Font.BOLD, 18);

    // Auswahl-Status: Welche Zeile ist gerade aktiv/markiert? (-1 = keine)
    private int selectedRow = -1;

    /**
     * Konstruktor: Initialisiert die Arrays und setzt Grundeinstellungen.
     */
    public SplitFlapDisplay(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        // Arrays initialisieren (Größe festlegen)
        this.current = new char[rows][cols];
        this.target = new char[rows][cols];

        // Startzustand: Alles leer (Leerzeichen)
        fill(this.current, ' ');
        fill(this.target, ' ');

        // Wichtig für Custom Components:
        // setOpaque(false) bedeutet, dass wir nicht den ganzen Hintergrund rechteckig ausmalen.
        // Das erlaubt Transparenz oder runde Ecken.
        setOpaque(false);
        setFont(flapFont);
    }

    // Getter für Dimensionen
    public int getRows() { return rows; }
    public int getCols() { return cols; }

    /**
     * Setzt die visuell markierte Zeile.
     * Nutzt repaint(), um die Änderung sofort sichtbar zu machen.
     */
    public void setSelectedRow(int row) {
        // Validierung: Wenn index ungültig, dann -1 (nichts selektiert)
        int next = (row < 0 || row >= rows) ? -1 : row;

        // Nur neu zeichnen, wenn sich wirklich was geändert hat (Performance)
        if (next != this.selectedRow) {
            this.selectedRow = next;
            repaint(); // Sagt Swing: "Bitte ruf bald paintComponent() auf"
        }
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    /**
     * Ermittelt, welche Zeile sich unter einem Maus-Punkt befindet.
     * Wichtig für Mausklicks (Hit-Testing).
     * * @param p Der Punkt (x, y) relativ zur Komponente.
     * @return Der Zeilen-Index oder -1, wenn daneben geklickt wurde.
     */
    public int rowAtPoint(Point p) {
        if (p == null) return -1;

        // Gesamthöhe einer Zeile inkl. Lücke
        int rowH = cellH + gapY;

        if (p.y < 0) return -1;

        // Einfache Division: Y-Koordinate durch Zeilenhöhe
        int r = p.y / rowH;

        if (r < 0 || r >= rows) return -1;

        // Detailprüfung: Haben wir in die Lücke (gapY) geklickt?
        // localY ist die Position innerhalb der theoretischen Zeile.
        int localY = p.y % rowH;

        // Wenn localY größer als die Zellhöhe ist, sind wir im "Niemandsland" zwischen den Zeilen.
        if (localY >= cellH) return -1;

        return r;
    }

    // Setzt die Größe der Zellen und fordert Layout-Neuberechnung an.
    public void setCellSize(int w, int h) {
        this.cellW = Math.max(8, w);
        this.cellH = Math.max(10, h);
        revalidate(); // Sagt dem LayoutManager: "Meine Größe hat sich geändert!"
        repaint();    // Neu zeichnen
    }

    // Setzt Timing-Parameter für die Animation
    public void setTiming(int ticksPerChar, int rowStaggerTicks) {
        this.ticksPerChar = Math.max(1, ticksPerChar);
        this.rowStaggerTicks = Math.max(0, rowStaggerTicks);
    }

    public void setMaxAdvancesPerTick(int maxAdvancesPerTick) {
        this.maxAdvancesPerTick = Math.max(1, maxAdvancesPerTick);
    }

    /**
     * Startet die Animation zu einem neuen Text.
     * Der Text wird auf die Zeilen verteilt und die Ziel-Buchstaben (target) werden gesetzt.
     */
    public void showTextRattle(String text) {
        // Hilfsmethode, um den langen Text in passende Happen für die Zeilen zu zerlegen
        List<String> lines = wrapToLines(text == null ? "" : text, rows, cols);

        // Wir aktualisieren NUR das 'target' Array.
        // Das 'current' Array bleibt noch alt. Der Timer erledigt den Rest.
        for (int r = 0; r < rows; r++) {
            String line = (r < lines.size()) ? lines.get(r) : "";
            for (int c = 0; c < cols; c++) {
                char ch = (c < line.length()) ? line.charAt(c) : ' ';
                target[r][c] = normalize(ch); // Sicherstellen, dass das Zeichen im Charset ist
            }
        }
        startTimer();
    }

    /**
     * Setzt alles auf Leerzeichen zurück.
     */
    public void clearRattle() {
        fill(target, ' '); // Ziel ist "alles leer"
        startTimer();
    }

    // Startet den Swing-Timer (ca. 30 FPS -> 33ms Delay)
    private void startTimer() {
        tick = 0;
        // Wenn schon ein Timer läuft, stoppen wir ihn, um Chaos zu vermeiden.
        if (timer != null) timer.stop();

        // Lambda-Ausdruck: Was soll alle 33ms passieren? -> step() aufrufen.
        timer = new Timer(33, e -> step());
        timer.start();
    }

    /**
     * Ein einzelner Schritt der Animation (wird vom Timer aufgerufen).
     * Hier wird berechnet, welches Zeichen als nächstes angezeigt wird.
     */
    private void step() {
        boolean anyChange = false; // Haben wir noch Arbeit oder sind wir fertig?

        for (int r = 0; r < rows; r++) {
            // Verzögerungseffekt: Zeile 1 startet später als Zeile 0
            int rowStart = r * rowStaggerTicks;
            if (tick < rowStart) continue; // Diese Zeile ist noch nicht dran

            // Wie weit ist diese Zeile schon?
            int localTick = tick - rowStart;

            // Effekt: Nicht alle Spalten starten gleichzeitig, sondern wellenartig von links nach rechts
            int activeCols = Math.min(cols, (localTick / ticksPerChar) + 1);

            for (int c = 0; c < activeCols; c++) {
                char cur = current[r][c];
                char tgt = target[r][c];

                // Wenn wir schon beim Ziel sind, nichts tun
                if (cur == tgt) continue;

                // Logik: Wir drehen das Rad weiter
                int dist = distanceForward(cur, tgt);
                // Um es schneller zu machen, springen wir ggf. mehrere Zeichen auf einmal
                int steps = Math.min(dist, maxAdvancesPerTick);

                // Rad drehen
                for (int i = 0; i < steps; i++) {
                    cur = nextChar(cur);
                }

                current[r][c] = cur; // Neuen Zustand speichern
                anyChange = true;    // Es hat sich was bewegt -> wir sind noch nicht fertig
            }

            // Falls in den hinteren Spalten (die noch nicht "aktiv" waren) noch Unterschiede sind,
            // merken wir uns das, damit der Timer nicht zu früh stoppt.
            for (int c = 0; c < cols; c++) {
                if (current[r][c] != target[r][c]) {
                    anyChange = true;
                    break;
                }
            }
        }

        tick++;
        repaint(); // Ganz wichtig: Swing auffordern, das Bild neu zu malen!

        // Wenn alles am Ziel ist, Timer stoppen, um CPU zu sparen.
        if (!anyChange && timer != null) {
            timer.stop();
            timer = null;
        }
    }

    // Berechnet die Distanz zwischen zwei Zeichen im Rad (nur vorwärts!)
    private static int distanceForward(char from, char to) {
        int a = CHARSET.indexOf(normalize(from));
        int b = CHARSET.indexOf(normalize(to));
        if (a < 0) a = 0;
        if (b < 0) b = 0;

        int n = CHARSET.length();
        int d = b - a;
        // Wenn das Ziel "kleiner" ist (z.B. von 'Z' nach 'A'), müssen wir über den Überlauf rechnen.
        if (d < 0) d += n;
        return d;
    }

    // Hilfsmethode: Array füllen
    private static void fill(char[][] a, char ch) {
        for (int r = 0; r < a.length; r++) {
            for (int c = 0; c < a[r].length; c++) {
                a[r][c] = ch;
            }
        }
    }

    /**
     * Textumbruch-Logik: Versucht, Wörter nicht zu zerschneiden.
     */
    private static List<String> wrapToLines(String text, int maxLines, int lineWidth) {
        String t = text.replace('\r', ' ').replace('\n', ' ').strip();
        List<String> out = new ArrayList<>();
        if (t.isEmpty()) {
            out.add("");
            return out;
        }

        int i = 0;
        while (i < t.length() && out.size() < maxLines) {
            // Nimm so viele Zeichen wie in eine Zeile passen
            int end = Math.min(t.length(), i + lineWidth);
            String chunk = t.substring(i, end);

            // Wenn wir nicht am Ende sind, versuchen wir, beim letzten Leerzeichen umzubrechen
            if (end < t.length()) {
                int lastSpace = chunk.lastIndexOf(' ');
                // Aber nur, wenn das Wort nicht zu lang ist (mehr als halbe Zeile)
                if (lastSpace >= Math.max(0, lineWidth / 2)) {
                    chunk = chunk.substring(0, lastSpace);
                    end = i + lastSpace + 1; // +1 um das Leerzeichen zu überspringen
                }
            }

            out.add(chunk.stripTrailing());
            i = end;

            // Führende Leerzeichen der nächsten Zeile überspringen
            while (i < t.length() && t.charAt(i) == ' ') i++;
        }
        return out;
    }

    // Macht Buchstaben groß und ersetzt unbekannte Zeichen durch Leerzeichen
    private static char normalize(char ch) {
        char up = Character.toUpperCase(ch);
        return CHARSET.indexOf(up) >= 0 ? up : ' ';
    }

    // Liefert das nächste Zeichen im Rad
    private static char nextChar(char current) {
        int idx = CHARSET.indexOf(normalize(current));
        if (idx < 0) idx = 0;
        idx = (idx + 1) % CHARSET.length(); // Modulo für den "Kreis"-Effekt
        return CHARSET.charAt(idx);
    }

    /**
     * Sagt dem LayoutManager (z.B. BorderLayout), wie groß dieses Element sein möchte.
     * Wird berechnet aus Anzahl Zeilen/Spalten + Zellgröße + Abstände.
     */
    @Override
    public Dimension getPreferredSize() {
        int w = cols * cellW + (cols - 1) * gapX;
        int h = rows * cellH + (rows - 1) * gapY;
        return new Dimension(w, h);
    }

    /**
     * ZEICHEN-METHODE. Hier passiert die Magie der Darstellung.
     * Wird automatisch von Swing aufgerufen, wenn repaint() getriggert wurde.
     */
    @Override
    protected void paintComponent(Graphics g) {
        // Graphics-Objekt kopieren und zu Graphics2D casten für bessere Optionen
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Anti-Aliasing einschalten für weiche Kanten (Text und Formen)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g2.setFont(getFont());

            // Schleife durch alle definierten Zeilen und Spalten
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    // X/Y Position berechnen
                    int x = c * (cellW + gapX);
                    int y = r * (cellH + gapY);

                    // 1. Hintergrund der Kachel (Dunkelgrau, leicht transparent)
                    g2.setColor(new Color(10, 10, 10, 200));
                    g2.fillRoundRect(x, y, cellW, cellH, 6, 6); // Abgerundete Ecken

                    // 2. Ein kleiner Glanz-Effekt (horizontale Linie in der Mitte)
                    // Das simuliert den Schlitz der mechanischen Plättchen.
                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.drawLine(x + 3, y + cellH / 2, x + cellW - 4, y + cellH / 2);

                    // 3. Den Buchstaben zentriert zeichnen
                    String s = String.valueOf(current[r][c]); // Aktuelles Zeichen aus dem Array
                    FontMetrics fm = g2.getFontMetrics();

                    // Zentrierung berechnen:
                    int tx = x + (cellW - fm.stringWidth(s)) / 2;
                    // Font-Höhe ist tricky: Ascent ist der Teil oberhalb der Grundlinie.
                    int ty = y + (cellH - fm.getHeight()) / 2 + fm.getAscent();

                    g2.setColor(new Color(245, 245, 245)); // Fast Weiß
                    g2.drawString(s, tx, ty);
                }

                // VISUALISIERUNG DER AUSWAHL
                // Wenn diese Zeile die 'selectedRow' ist, malen wir einen Rahmen drumherum.
                if (r == selectedRow) {
                    int rowX = 0;
                    int rowY = r * (cellH + gapY);
                    int rowW = cols * cellW + (cols - 1) * gapX;
                    int rowH = cellH; // Rahmen ist so hoch wie eine Zelle

                    // Orange, halbtransparent
                    g2.setColor(new Color(255, 210, 120, 150));
                    g2.setStroke(new BasicStroke(2f)); // Dickere Linie
                    // Etwas größer als die Zellen zeichnen (-2 offset, +4 größe)
                    g2.drawRoundRect(rowX - 2, rowY - 2, rowW + 4, rowH + 4, 10, 10);
                }
            }
        } finally {
            // WICHTIG: Erzeugte Graphics-Objekte immer entsorgen (Speicherleck-Prävention)
            g2.dispose();
        }
    }
}