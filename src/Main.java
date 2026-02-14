import javax.swing.*;

/**
 * DE:
 * Einstiegspunkt der Anwendung. Diese Klasse startet die Swing-GUI auf dem
 * Event-Dispatch-Thread (EDT), wie es in Swing zwingend empfohlen ist.
 *
 * EN:
 * Application entry point. This class starts the Swing UI on the
 * Event Dispatch Thread (EDT), which is the recommended and safe way in Swing.
 *
 * Warum EDT?
 * DE: Swing ist nicht thread-sicher. Alle UI-Operationen sollen im EDT laufen.
 * EN: Swing is not thread-safe. All UI work should happen on the EDT.
 */
public class Main {

    /**
     * DE:
     * Startet die Anwendung. Erstellt ein {@link JFrame}, setzt das Haupt-Panel
     * ({@link CalendarMachinePanel}) als ContentPane und macht das Fenster sichtbar.
     *
     * EN:
     * Starts the application. Creates a {@link JFrame}, sets the main panel
     * ({@link CalendarMachinePanel}) as content pane and shows the window.
     */
    public static void main(String[] args) {
        // DE/EN: invokeLater stellt sicher, dass die GUI-Erstellung im EDT passiert.
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Zeitmaschine Kalender");

            // DE: Prozess beenden, wenn das Hauptfenster geschlossen wird.
            // EN: Exit the JVM when the main window is closed.
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // DE: Unser Hauptpanel enthält die komplette „Maschinen“-UI.
            // EN: The main panel contains the full “machine” UI.
            frame.setContentPane(new CalendarMachinePanel());

            // DE: pack() nimmt PreferredSize der Inhalte (z.B. Canvas-Größe) als Grundlage.
            // EN: pack() sizes the frame based on preferred sizes of the content.
            frame.pack();

            // DE: Feste Größe – passt zum Pixel-Art/Assets-Layout.
            // EN: Fixed size – matches the pixel-perfect asset layout.
            frame.setResizable(false);

            // DE/EN: Zentriert auf dem Bildschirm.
            frame.setLocationRelativeTo(null);

            frame.setVisible(true);
        });
    }
}