package DayEntriesWindow;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DayEntriesWindow extends JDialog {
    private final Map<LocalDate, List<String>> entryStore;
    private final Runnable onStoreChanged;

    private final JTextField input = new JTextField();

    private final JButton addButton = new JButton("+");
    private final JButton editButton = new JButton("\u270E");
    private final JButton deleteButton = new JButton("\u2212");

    private LocalDate currentDate;

    private final Font chalkFont;

    private final SplitFlapDisplay flap = new SplitFlapDisplay(6, 16);

    // Auswahl in der 6-Zeilen-Anzeige
    private int selectedRow = -1;
    private String selectedOriginalText = "";

    // Aktuell angezeigte 6 Zeilen inkl. Verweis auf die Original-Quelle im Store
    private final List<EntryRef> lastShown = new ArrayList<>();

    // Kontextmenü: Wiederholung ändern
    private final JPopupMenu repeatMenu = new JPopupMenu();
    private final ButtonGroup repeatGroup = new ButtonGroup();
    private final java.util.EnumMap<RepeatMode, JRadioButtonMenuItem> repeatItems =
            new java.util.EnumMap<>(RepeatMode.class);

    private enum RepeatMode {
        NONE("Keine", ""),
        YEARLY("Jährlich", "[R:YEAR]"),
        WEEKDAYS("Mo–Fr", "[R:WKD]"),
        MONTHLY("Monatlich", "[R:MON]");

        final String label;
        final String prefix;

        RepeatMode(String label, String prefix) {
            this.label = label;
            this.prefix = prefix;
        }
    }

    private static final class EntryRef {
        final LocalDate originDate;
        final int originIndex;
        final String storedText;

        private EntryRef(LocalDate originDate, int originIndex, String storedText) {
            this.originDate = originDate;
            this.originIndex = originIndex;
            this.storedText = storedText;
        }
    }

    public DayEntriesWindow(Window owner, Map<LocalDate, List<String>> entryStore, Runnable onStoreChanged) {
        super(owner, "Einträge", ModalityType.MODELESS);
        this.entryStore = entryStore;
        this.onStoreChanged = (onStoreChanged != null) ? onStoreChanged : () -> {};

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DayEntriesWindow.this.onStoreChanged.run();
            }
        });

        chalkFont = pickChalkFont(22f);

        // ===== Eingabe + Buttons =====
        input.setFont(chalkFont.deriveFont(18f));
        input.setForeground(Color.WHITE);
        input.setCaretColor(Color.WHITE);
        input.setOpaque(true);
        input.setBackground(new Color(0, 0, 0, 140));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 60)),
                new EmptyBorder(6, 8, 6, 8)
        ));

        // Enter = ändern (wenn Auswahl), sonst hinzufügen
        input.addActionListener(e -> {
            if (getSelectedRef() != null) {
                editSelectedInline();
            } else {
                addEntryFromInput();
            }
        });

        // Key bindings: Esc (Abbrechen), Ctrl+Enter (immer hinzufügen)
        InputMap im = input.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = input.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelEdit");
        am.put("cancelEdit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedRow >= 0) {
                    input.setText(selectedOriginalText == null ? "" : selectedOriginalText);
                    input.selectAll();
                } else {
                    input.setText("");
                }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "forceAdd");
        am.put("forceAdd", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addEntryFromInput();
            }
        });

        addButton.setToolTipText("Hinzufügen");
        editButton.setToolTipText("Ändern");
        deleteButton.setToolTipText("Löschen");

        styleIconButton(addButton);
        styleIconButton(editButton);
        styleIconButton(deleteButton);

        addButton.addActionListener(e -> addEntryFromInput());
        editButton.addActionListener(e -> editSelectedInline());
        deleteButton.addActionListener(e -> deleteSelectedInline());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);

        // ===== Hintergrund =====
        BufferedImage brass = tryLoadImageFile("assets/MessingTafel.png");
        JPanel root = new BrassBackgroundPanel(brass);
        root.setLayout(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        // ===== Split-Flap =====
        flap.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        flap.setTiming(3, 3);
        flap.setMaxAdvancesPerTick(10);
        flap.setCellSize(26, 32);

        buildRepeatMenu();

        flap.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleFlapMouse(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleFlapMouse(e);
            }

            private void handleFlapMouse(MouseEvent e) {
                int row = flap.rowAtPoint(e.getPoint());

                if (row >= 0) {
                    boolean focusEdit = (!e.isPopupTrigger() && e.getClickCount() >= 2);
                    selectRow(row, focusEdit);
                }

                if (e.isPopupTrigger()) {
                    // Rechtsklick ohne gültige Zeile => Menü nicht anzeigen
                    if (row < 0) return;

                    updateRepeatMenuChecks();
                    repeatMenu.show(flap, e.getX(), e.getY());
                }
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        top.setOpaque(false);
        top.add(flap);

        root.add(top, BorderLayout.NORTH);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
        setMinimumSize(new Dimension(520, 260));
        pack();
    }

    // ===================== Kontextmenü Wiederholung =====================

    private void buildRepeatMenu() {
        repeatMenu.removeAll();
        repeatGroup.clearSelection();
        repeatItems.clear();

        JMenuItem title = new JMenuItem("Wiederholung:");
        title.setEnabled(false);
        repeatMenu.add(title);
        repeatMenu.addSeparator();

        addRepeatMenuItem(RepeatMode.NONE);
        addRepeatMenuItem(RepeatMode.YEARLY);
        addRepeatMenuItem(RepeatMode.WEEKDAYS);
        addRepeatMenuItem(RepeatMode.MONTHLY);
    }

    private void addRepeatMenuItem(RepeatMode mode) {
        JRadioButtonMenuItem it = new JRadioButtonMenuItem(mode.label);
        repeatGroup.add(it);
        repeatItems.put(mode, it);

        it.addActionListener(e -> changeRepeatModeOfSelected(mode));
        repeatMenu.add(it);
    }

    private void updateRepeatMenuChecks() {
        EntryRef ref = getSelectedRef();
        boolean enabled = (ref != null);

        RepeatMode current = enabled ? detectRepeatModeFromStored(ref.storedText) : RepeatMode.NONE;

        for (RepeatMode m : RepeatMode.values()) {
            JRadioButtonMenuItem it = repeatItems.get(m);
            if (it != null) {
                it.setEnabled(enabled);
                it.setSelected(m == current);
            }
        }
    }

    private void changeRepeatModeOfSelected(RepeatMode newMode) {
        EntryRef ref = getSelectedRef();
        if (ref == null) return;

        List<String> items = entryStore.get(ref.originDate);
        if (items == null || ref.originIndex < 0 || ref.originIndex >= items.size()) return;

        String base = stripRepeatPrefixForDisplay(items.get(ref.originIndex));
        String updatedStored = applyRepeatPrefixForStorage(base, newMode);
        items.set(ref.originIndex, updatedStored);

        // UI aktualisieren (Text im Feld bleibt der Basistext)
        selectedOriginalText = base;
        input.setText(base);
        input.selectAll();
        updateFlapFromStore(true);

        onStoreChanged.run();
    }

    // ===================== Wiederholung: speichern & anwenden =====================

    private RepeatMode askRepeatMode() {
        Object[] options = {
                RepeatMode.NONE.label,
                RepeatMode.YEARLY.label,
                RepeatMode.WEEKDAYS.label,
                RepeatMode.MONTHLY.label
        };

        int res = JOptionPane.showOptionDialog(
                this,
                "Wiederholung für diesen Termin?",
                "Wiederholung",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        return switch (res) {
            case 1 -> RepeatMode.YEARLY;
            case 2 -> RepeatMode.WEEKDAYS;
            case 3 -> RepeatMode.MONTHLY;
            default -> RepeatMode.NONE;
        };
    }

    private static String applyRepeatPrefixForStorage(String text, RepeatMode mode) {
        String t = (text == null) ? "" : text.trim();
        if (t.isEmpty() || mode == null || mode == RepeatMode.NONE) return t;
        return mode.prefix + " " + t;
    }

    private static RepeatMode detectRepeatModeFromStored(String stored) {
        if (stored == null) return RepeatMode.NONE;
        String s = stored.stripLeading();
        if (s.startsWith(RepeatMode.YEARLY.prefix)) return RepeatMode.YEARLY;
        if (s.startsWith(RepeatMode.WEEKDAYS.prefix)) return RepeatMode.WEEKDAYS;
        if (s.startsWith(RepeatMode.MONTHLY.prefix)) return RepeatMode.MONTHLY;
        return RepeatMode.NONE;
    }

    private static String stripRepeatPrefixForDisplay(String stored) {
        if (stored == null) return "";
        String s = stored.stripLeading();

        for (RepeatMode m : RepeatMode.values()) {
            if (!m.prefix.isEmpty() && s.startsWith(m.prefix)) {
                return s.substring(m.prefix.length()).stripLeading();
            }
        }
        return stored;
    }

    private static boolean appliesToDate(RepeatMode mode, LocalDate origin, LocalDate date) {
        if (mode == null || origin == null || date == null) return false;

        return switch (mode) {
            case NONE -> false;
            case YEARLY -> origin.getMonthValue() == date.getMonthValue() && origin.getDayOfMonth() == date.getDayOfMonth();
            case MONTHLY -> origin.getDayOfMonth() == date.getDayOfMonth();
            case WEEKDAYS -> {
                DayOfWeek dow = date.getDayOfWeek();
                yield dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            }
        };
    }

    // ===================== Auswahl / Anzeige =====================

    private void selectRow(int row, boolean focusEdit) {
        selectedRow = row;
        flap.setSelectedRow(row);

        EntryRef ref = getSelectedRef();
        String display = (ref == null) ? "" : stripRepeatPrefixForDisplay(ref.storedText);
        selectedOriginalText = display;

        input.setText(display);
        if (focusEdit) {
            input.requestFocusInWindow();
            input.selectAll();
        }

        updateButtonsEnabled();
    }

    private EntryRef getSelectedRef() {
        if (selectedRow < 0 || selectedRow >= lastShown.size()) return null;
        return lastShown.get(selectedRow);
    }

    private void updateButtonsEnabled() {
        boolean hasSelection = (getSelectedRef() != null);
        editButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
    }

    private void rebuildLastShownForCurrentDate() {
        lastShown.clear();
        if (currentDate == null) return;

        // 1) direkte Einträge des Tages
        List<String> direct = entryStore.getOrDefault(currentDate, List.of());
        for (int i = 0; i < direct.size(); i++) {
            lastShown.add(new EntryRef(currentDate, i, direct.get(i)));
        }

        // 2) Wiederholer von anderen Tagen, die auf currentDate zutreffen
        for (Map.Entry<LocalDate, List<String>> e : entryStore.entrySet()) {
            LocalDate origin = e.getKey();
            if (origin == null || origin.equals(currentDate)) continue;

            List<String> items = e.getValue();
            if (items == null) continue;

            for (int i = 0; i < items.size(); i++) {
                String stored = items.get(i);
                RepeatMode mode = detectRepeatModeFromStored(stored);
                if (mode == RepeatMode.NONE) continue;

                if (appliesToDate(mode, origin, currentDate)) {
                    lastShown.add(new EntryRef(origin, i, stored));
                }
            }
        }

        // Nur letzte 6 zeigen
        if (lastShown.size() > 6) {
            int from = lastShown.size() - 6;
            List<EntryRef> tail = new ArrayList<>(lastShown.subList(from, lastShown.size()));
            lastShown.clear();
            lastShown.addAll(tail);
        }
    }

    private void updateFlapFromStore(boolean animate) {
        rebuildLastShownForCurrentDate();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i > 0) sb.append('\n');
            if (i < lastShown.size()) {
                sb.append(stripRepeatPrefixForDisplay(lastShown.get(i).storedText));
            } else {
                sb.append("");
            }
        }

        // (animate wird aktuell immer "ratternd" umgesetzt)
        flap.showTextRattle(sb.toString());

        // Auswahl ggf. korrigieren
        if (selectedRow >= lastShown.size()) {
            selectedRow = -1;
            selectedOriginalText = "";
            flap.setSelectedRow(-1);
        }
        updateButtonsEnabled();
    }

    // ===================== CRUD =====================

    private void addEntryFromInput() {
        if (currentDate == null) return;

        String text = input.getText().trim();
        if (text.isEmpty()) return;

        RepeatMode mode = askRepeatMode();
        String stored = applyRepeatPrefixForStorage(text, mode);

        entryStore.computeIfAbsent(currentDate, d -> new ArrayList<>()).add(stored);

        input.setText("");

        updateFlapFromStore(true);

        // neue Auswahl auf letzte sichtbare Zeile
        if (!lastShown.isEmpty()) {
            selectRow(Math.min(5, lastShown.size() - 1), false);
        }

        onStoreChanged.run();
    }

    private void editSelectedInline() {
        EntryRef ref = getSelectedRef();
        if (ref == null) return;

        String newText = input.getText().trim();
        if (newText.isEmpty()) return;

        // Wiederholungsmodus beibehalten
        RepeatMode mode = detectRepeatModeFromStored(ref.storedText);
        String newStored = applyRepeatPrefixForStorage(newText, mode);

        List<String> items = entryStore.get(ref.originDate);
        if (items == null || ref.originIndex < 0 || ref.originIndex >= items.size()) return;

        items.set(ref.originIndex, newStored);

        selectedOriginalText = newText;

        updateFlapFromStore(true);
        flap.setSelectedRow(selectedRow);

        onStoreChanged.run();
    }

    private void deleteSelectedInline() {
        EntryRef ref = getSelectedRef();
        if (ref == null) return;

        List<String> items = entryStore.get(ref.originDate);
        if (items == null || ref.originIndex < 0 || ref.originIndex >= items.size()) return;

        items.remove(ref.originIndex);
        if (items.isEmpty()) entryStore.remove(ref.originDate);

        input.setText("");
        selectedOriginalText = "";

        updateFlapFromStore(true);

        if (lastShown.isEmpty()) {
            selectedRow = -1;
            flap.setSelectedRow(-1);
            updateButtonsEnabled();
        } else {
            int newRow = Math.min(selectedRow, lastShown.size() - 1);
            selectRow(newRow, false);
        }

        onStoreChanged.run();
    }

    // ===================== Window API =====================

    public void showForDate(LocalDate selected) {
        this.currentDate = selected;
        setTitle("Einträge für " + selected);

        selectedRow = -1;
        selectedOriginalText = "";
        flap.setSelectedRow(-1);

        input.setText("");
        updateFlapFromStore(true);

        if (!isVisible()) setVisible(true);
        toFront();
        requestFocus();
    }

    // ===================== Helpers / UI =====================

    private static BufferedImage tryLoadImageFile(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) return null;
            return ImageIO.read(f);
        } catch (IOException ignored) {
            return null;
        }
    }

    private void styleIconButton(JButton b) {
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        b.setFocusable(false);
        b.setMargin(new Insets(8, 14, 8, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.setForeground(Color.WHITE);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
    }

    private static Font pickChalkFont(float size) {
        String[] preferred = {"Segoe Print", "Segoe Script", "Comic Sans MS"};
        for (String name : preferred) {
            Font f = new Font(name, Font.PLAIN, Math.round(size));
            if (f.canDisplay('ä') && f.canDisplay('ß')) {
                if (f.getFamily().equalsIgnoreCase(name)
                        || f.getFontName().toLowerCase().contains(name.toLowerCase())) {
                    return f.deriveFont(size);
                }
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(size)).deriveFont(size);
    }

    private static final class BrassBackgroundPanel extends JPanel {
        private final BufferedImage bg;

        private BrassBackgroundPanel(BufferedImage bg) {
            this.bg = bg;
            setOpaque(true);
            setBackground(new Color(35, 28, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (bg == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int w = getWidth();
                int h = getHeight();
                g2.drawImage(bg, 0, 0, w, h, null);

                g2.setColor(new Color(0, 0, 0, 25));
                g2.fillRect(0, 0, w, h);
            } finally {
                g2.dispose();
            }
        }
    }
}