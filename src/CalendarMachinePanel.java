import DayEntriesWindow.DayEntriesWindow;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarMachinePanel extends JPanel {
    private final JLayeredPane layers = new JLayeredPane();

    // Hinterste Ebene
    private final RotatableSprite zeitMaschiene;
    private final RotatableSprite zahnrad1;
    private final RotatableSprite zahnrad2;

    // Ebene 2: Glaselemente + Zahnräder + Effekte
    private final RotatableSprite glaselementeMitRohren;
    private final RotatableSprite zahnrad3;
    private final RotatableSprite zahnrad4;
    private final RotatableSprite zahnrad5;

    // Ebene 3 davor
    private final RotatableSprite zahnrad7;
    private final RotatableSprite zahnrad6;

    // Ebene davor
    private final RotatableSprite zeitMaschieneOhneHebel;

    // Ebene 4 vorne
    private final BullaugeControl rechtesBullauge;
    private final BullaugeControl mittleresBullauge;
    private final BullaugeControl linkesBullauge;
    private final LeverControl hebel;

    private static final int CANVAS_W = 534;
    private static final int CANVAS_H = 773;

    // Effekte
    private final FlickerLight light1 = new FlickerLight(new Color(255, 190, 80));
    private final FlickerLight light2 = new FlickerLight(new Color(255, 220, 120));
    private final FlickerLight light3 = new FlickerLight(new Color(255, 170, 60));

    private final SteamCloud steamSmall = new SteamCloud();
    private final SteamCloud steamMedium = new SteamCloud();
    private final SteamCloud steamBigFront = new SteamCloud();

    // Speicher (in RAM)
    private final Map<LocalDate, List<String>> entryStore = new HashMap<>();

    // Neu: für leichtes „Cascading“, damit neue Fenster nicht exakt übereinander liegen
    private int dayWindowOpenCount = 0;

    public CalendarMachinePanel() {
        setLayout(new BorderLayout());
        add(layers, BorderLayout.CENTER);
        layers.setLayout(null);

        // gespeicherte Einträge laden
        entryStore.putAll(EntryStoreIO.loadOrEmpty());

        LocalDate now = LocalDate.now();

        var imgZeitMaschiene = ImageLoader.loadOrThrow("assets/ZeitMaschiene.png");
        var imgZeitMaschieneOhneHebel = ImageLoader.loadOrThrow("assets/ZeitMaschieneOhneHebel.png");
        var imgGlas = ImageLoader.loadOrThrow("assets/GlaselementeMitRohren.png");

        zeitMaschiene = new RotatableSprite(imgZeitMaschiene);

        zahnrad1 = new RotatableSprite(ImageLoader.loadOrThrow("assets/Zahnrad1.png"));
        zahnrad2 = new RotatableSprite(ImageLoader.loadOrThrow("assets/Zahnrad2.png"));

        glaselementeMitRohren = new RotatableSprite(imgGlas);

        zahnrad3 = new RotatableSprite(ImageLoader.loadOrThrow("assets/Zahnrad3.png"));
        zahnrad4 = new RotatableSprite(ImageLoader.loadOrThrow("assets/Zahnrad4.png"));

        var imgZahnradKlein = ImageLoader.loadOrThrow("assets/ZahnradKlein.png");
        zahnrad5 = new RotatableSprite(imgZahnradKlein);
        zahnrad6 = new RotatableSprite(imgZahnradKlein);
        zahnrad7 = new RotatableSprite(imgZahnradKlein);

        zeitMaschieneOhneHebel = new RotatableSprite(imgZeitMaschieneOhneHebel);

        rechtesBullauge = new BullaugeControl(
                ImageLoader.loadOrThrow("assets/rechtesBullauge.png"),
                new FlipNumberDisplay(now.getYear(), 0, 9999, FlipNumberDisplay.WrapMode.CLAMP)
        );
        mittleresBullauge = new BullaugeControl(
                ImageLoader.loadOrThrow("assets/mittleresBullauge.png"),
                new FlipNumberDisplay(now.getMonthValue(), 1, 12, FlipNumberDisplay.WrapMode.WRAP)
        );
        linkesBullauge = new BullaugeControl(
                ImageLoader.loadOrThrow("assets/linkesBullauge.png"),
                new FlipNumberDisplay(now.getDayOfMonth(), 1, 31, FlipNumberDisplay.WrapMode.WRAP)
        );

        hebel = new LeverControl(ImageLoader.loadOrThrow("assets/Hebel.png"));

        setPreferredSize(new Dimension(CANVAS_W, CANVAS_H));

        // Ebenen
        final int L0_BACK = 0;
        final int L1_LAYER2 = 100;  // Ebene 2 (Lichter + 2 Dampf)
        final int L2_LAYER3 = 200;
        final int L3_LAYER = 300;
        final int L4_FRONT = 400;   // Ebene 4 (großer Dampf + UI)

        layers.add(zeitMaschiene, Integer.valueOf(L0_BACK));
        layers.add(zahnrad1, Integer.valueOf(L0_BACK));
        layers.add(zahnrad2, Integer.valueOf(L0_BACK));

        layers.add(glaselementeMitRohren, Integer.valueOf(L1_LAYER2));
        layers.add(zahnrad3, Integer.valueOf(L1_LAYER2));
        layers.add(zahnrad4, Integer.valueOf(L1_LAYER2));
        layers.add(zahnrad5, Integer.valueOf(L1_LAYER2));

        // Ebene 2 Effekte
        layers.add(light1, Integer.valueOf(L1_LAYER2));
        layers.add(light2, Integer.valueOf(L1_LAYER2));
        layers.add(light3, Integer.valueOf(L1_LAYER2));
        layers.add(steamSmall, Integer.valueOf(L1_LAYER2));
        layers.add(steamMedium, Integer.valueOf(L1_LAYER2));

        layers.add(zahnrad7, Integer.valueOf(L2_LAYER3));
        layers.add(zahnrad6, Integer.valueOf(L2_LAYER3));

        layers.add(zeitMaschieneOhneHebel, Integer.valueOf(L3_LAYER));

        layers.add(rechtesBullauge, Integer.valueOf(L4_FRONT));
        layers.add(mittleresBullauge, Integer.valueOf(L4_FRONT));
        layers.add(linkesBullauge, Integer.valueOf(L4_FRONT));
        layers.add(hebel, Integer.valueOf(L4_FRONT));

        // Ebene 4 Effekt (großer Dampf vorne)
        layers.add(steamBigFront, Integer.valueOf(L4_FRONT));

        // Mechanik
        rechtesBullauge.setOnStep(step -> {
            rotateGearPair(zahnrad1, zahnrad2, step, 6.0);
            normalizeDayToMonth();
        });

        mittleresBullauge.setOnStep(step -> {
            rotateGear(zahnrad3, step, 7.0);
            rotateGear(zahnrad4, -step, 7.0);
            rotateGear(zahnrad5, step, 5.0);
            normalizeDayToMonth();
        });

        linkesBullauge.setOnStep(step -> rotateGearPair(zahnrad6, zahnrad7, step, 6.0));

        // Startsequenz bei Hebel unten
        hebel.setOnLatchedBottom(this::startLeverSequence);

        normalizeDayToMonth();
    }

    private void normalizeDayToMonth() {
        int year = rechtesBullauge.getDisplay().getValue();
        int month = mittleresBullauge.getDisplay().getValue();
        int day = linkesBullauge.getDisplay().getValue();

        month = Math.max(1, Math.min(12, month));

        YearMonth ym;
        try {
            ym = YearMonth.of(Math.max(0, Math.min(9999, year)), month);
        } catch (Exception ex) {
            ym = YearMonth.now();
        }

        int maxDay = ym.lengthOfMonth();
        int clampedDay = Math.max(1, Math.min(maxDay, day));

        if (clampedDay != day) {
            linkesBullauge.getDisplay().setValue(clampedDay);
            linkesBullauge.repaint();
        }
    }

    private void startLeverSequence() {
        // Reset
        light1.stopFlicker();
        light2.stopFlicker();
        light3.stopFlicker();
        steamSmall.stopSteam();
        steamMedium.stopSteam();
        steamBigFront.stopSteam();

        // 1) 3 Lichter nacheinander
        Timer t1 = new Timer(0, e -> {
            ((Timer) e.getSource()).stop();
            light1.startFlicker();
        });
        t1.setRepeats(false);
        t1.start();

        Timer t2 = new Timer(350, e -> {
            ((Timer) e.getSource()).stop();
            light2.startFlicker();
        });
        t2.setRepeats(false);
        t2.start();

        Timer t3 = new Timer(700, e -> {
            ((Timer) e.getSource()).stop();
            light3.startFlicker();
        });
        t3.setRepeats(false);
        t3.start();

        // 2) Danach Dampf
        Timer tSteam = new Timer(1100, e -> {
            ((Timer) e.getSource()).stop();
            steamSmall.startSteam();
            steamMedium.startSteam();
            steamBigFront.startSteam();
        });
        tSteam.setRepeats(false);
        tSteam.start();

        // 3) Danach Fenster öffnen
        Timer tWin = new Timer(1600, e -> {
            ((Timer) e.getSource()).stop();
            openDayWindowTopRightNextToOwner();
        });
        tWin.setRepeats(false);
        tWin.start();
    }

    private void openDayWindowTopRightNextToOwner() {
        normalizeDayToMonth();

        LocalDate selected = getSelectedDateFromBullauges();

        Window owner = SwingUtilities.getWindowAncestor(this);
        if (owner == null) return;

        // Jedes Mal ein neues Fenster (mehrere parallel möglich)
        DayEntriesWindow dayWindow = new DayEntriesWindow(owner, entryStore, () -> EntryStoreIO.save(entryStore));

        int winW = 520;
        int winH = 260 + 100; // 100px höher

        Point p = owner.getLocationOnScreen();
        int gap = 12;

        int x = p.x + owner.getWidth() + gap;
        int y = p.y;

        // leicht versetzt, wenn mehrfach geöffnet
        int cascade = (dayWindowOpenCount++ % 8) * 24;
        x += cascade;
        y += cascade;

        dayWindow.setSize(winW, winH);
        dayWindow.setLocation(x, y);
        dayWindow.showForDate(selected);
    }

    private LocalDate getSelectedDateFromBullauges() {
        int year = rechtesBullauge.getDisplay().getValue();
        int month = mittleresBullauge.getDisplay().getValue();
        int day = linkesBullauge.getDisplay().getValue();

        month = Math.max(1, Math.min(12, month));
        YearMonth ym;
        try {
            ym = YearMonth.of(Math.max(1, Math.min(9999, year)), month);
        } catch (Exception ex) {
            ym = YearMonth.now();
        }

        int maxDay = ym.lengthOfMonth();
        day = Math.max(1, Math.min(maxDay, day));

        return LocalDate.of(ym.getYear(), ym.getMonthValue(), day);
    }

    private void rotateGearPair(RotatableSprite a, RotatableSprite b, int step, double degreesPerStep) {
        rotateGear(a, step, degreesPerStep);
        rotateGear(b, -step, degreesPerStep);
    }

    private void rotateGear(RotatableSprite gear, int step, double degreesPerStep) {
        double next = gear.getAngleRadians() + Math.toRadians(step * degreesPerStep);
        double twoPi = Math.PI * 2.0;
        next = next % twoPi;
        if (next < 0) next += twoPi;
        gear.setAngleRadians(next);
    }

    @Override
    public void doLayout() {
        super.doLayout();

        Insets insets = getInsets();
        int availW = Math.max(0, getWidth() - insets.left - insets.right);
        int availH = Math.max(0, getHeight() - insets.top - insets.bottom);
        layers.setBounds(insets.left, insets.top, availW, availH);

        // Full-canvas layers
        zeitMaschiene.setBounds(0, 0, CANVAS_W, CANVAS_H);
        glaselementeMitRohren.setBounds(0, 0, CANVAS_W, CANVAS_H);
        zeitMaschieneOhneHebel.setBounds(0, 0, CANVAS_W, CANVAS_H);

        // Bullaugen
        int bullSize = 130;
        int bullY = 415;
        int bullYOffset = -80;

        linkesBullauge.setBounds(20, bullY + bullYOffset, bullSize, bullSize);
        mittleresBullauge.setBounds(160, bullY + bullYOffset, bullSize, bullSize);
        rechtesBullauge.setBounds(295, bullY + bullYOffset, bullSize, bullSize);

        // Hebel
        hebel.setBounds(402, 225, 120, 420);
        hebel.setDragYRange(170, 360);

        // Zahnräder
        zahnrad1.setBounds(10, 50, 270, 270);
        zahnrad2.setBounds(10, 90, 220, 220);

        zahnrad3.setBounds(80, 150, 130, 130);
        zahnrad4.setBounds(190, 165, 110, 110);
        zahnrad5.setBounds(250, 250, 80, 80);

        zahnrad6.setBounds(299, 583, 60, 60);
        zahnrad7.setBounds(347, 603, 70, 70);

        // ===== Positionierung Effekte =====
        int margin = 32;

        // oben rechts
        int l1w = 235, l1h = 28;
        light1.setBounds(CANVAS_W - margin - l1w, margin, l1w, l1h);

        // unten links
        int l2w = 80, l2h = 30;
        int l3w = 180, l3h = 34;

        int baseX = margin;
        int baseY = CANVAS_H - margin;

        light2.setBounds(baseX, baseY - l2h, l2w, l2h);
        light3.setBounds(baseX + 22, baseY - l3h - 14, l3w, l3h);

        // Dampf
        steamSmall.setBounds(60, 140, 140, 140);
        steamMedium.setBounds(260, 220, 200, 200);
        steamBigFront.setBounds(150, 250, 360, 360);
    }
}