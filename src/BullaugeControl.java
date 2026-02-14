import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.IntConsumer;

public class BullaugeControl extends JComponent {
    private final BufferedImage frameImage;
    private final FlipNumberDisplay display;
    private IntConsumer onStep;

    private static final int STEPS_PER_NOTCH = 1;
    private static final int FAST_MULTIPLIER = 5;

    private final Timer flipTimer;
    private boolean flipping;
    private long flipStartNanos;
    private static final long FLIP_DURATION_NANOS = 180_000_000L;

    private int flipFrom;
    private int flipTo;
    private double flipProgress;

    private int windowWpx = 90;
    private int windowHpx = 90;

    private int windowOffsetXpx = 0;
    private int windowOffsetYpx = 0;

    private float fontRel = 0.30f;

    public BullaugeControl(BufferedImage frameImage, FlipNumberDisplay display) {
        this.frameImage = frameImage;
        this.display = Objects.requireNonNull(display);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        flipFrom = display.getValue();
        flipTo = display.getValue();

        flipTimer = new Timer(1000 / 60, e -> {
            long now = System.nanoTime();
            long dt = now - flipStartNanos;
            flipProgress = Math.min(1.0, dt / (double) FLIP_DURATION_NANOS);
            repaint();

            if (flipProgress >= 1.0) {
                flipping = false;
                ((Timer) e.getSource()).stop();

                // Neu: Nach der Animation den "stehenden" Wert aktualisieren
                flipFrom = flipTo;

                repaint();
            }
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                int step = -notches * STEPS_PER_NOTCH;
                if (e.isShiftDown()) step *= FAST_MULTIPLIER;

                if (step != 0) {
                    applyStep(step);
                    e.consume();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int step = SwingUtilities.isRightMouseButton(e) ? -1 : 1;
                applyStep(step);
            }
        };

        addMouseWheelListener(mouse);
        addMouseListener(mouse);
    }

    public void setWindowSizePx(int widthPx, int heightPx) {
        this.windowWpx = Math.max(1, widthPx);
        this.windowHpx = Math.max(1, heightPx);
        repaint();
    }

    public void setWindowOffsetFromCenterPx(int offsetXpx, int offsetYpx) {
        this.windowOffsetXpx = offsetXpx;
        this.windowOffsetYpx = offsetYpx;
        repaint();
    }

    public void setWindowStyle(int ignoredArc, float fontHeightFactor) {
        this.fontRel = fontHeightFactor;
        repaint();
    }

    public void setOnStep(IntConsumer onStep) {
        this.onStep = onStep;
    }

    public FlipNumberDisplay getDisplay() {
        return display;
    }

    private void applyStep(int step) {
        int oldValue = display.getValue();
        display.add(step);
        int newValue = display.getValue();

        if (onStep != null) onStep.accept(step);

        if (newValue != oldValue) startFlip(oldValue, newValue);
        else repaint();
    }

    private void startFlip(int from, int to) {
        flipFrom = from;
        flipTo = to;
        flipping = true;
        flipProgress = 0.0;
        flipStartNanos = System.nanoTime();

        if (flipTimer.isRunning()) flipTimer.stop();
        flipTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (frameImage != null) {
                g2.drawImage(frameImage, 0, 0, w, h, null);
            } else {
                g2.setColor(new Color(40, 40, 40));
                g2.fillOval(0, 0, w, h);
            }

            int centerX = w / 2;
            int centerY = h / 2;

            int diameter = Math.min(windowWpx, windowHpx);
            int winX = centerX - diameter / 2 + windowOffsetXpx;
            int winY = centerY - diameter / 2 + windowOffsetYpx;

            Ellipse2D circle = new Ellipse2D.Double(winX, winY, diameter, diameter);

            g2.setColor(new Color(10, 10, 10, 200));
            g2.fill(circle);

            int midY = winY + diameter / 2;
            g2.setColor(new Color(255, 255, 255, 40));
            g2.drawLine(winX + 6, midY, winX + diameter - 6, midY);

            // Neu: Im Ruhezustand immer den aktuellen Display-Wert anzeigen
            if (!flipping) {
                String text = formatValue(display.getValue());
                float fontSize = Math.max(12f, diameter * fontRel);
                g2.setFont(getFont().deriveFont(Font.BOLD, fontSize));
                FontMetrics fm = g2.getFontMetrics();

                int textCenterX = winX + diameter / 2;
                int textCenterY = winY + diameter / 2;
                int baseY = textCenterY + (fm.getAscent() - fm.getDescent()) / 2;

                Shape oldClip = g2.getClip();
                g2.setClip(circle);
                drawCenteredText(g2, text, textCenterX, baseY);
                g2.setClip(oldClip);
                return;
            }

            // Flip-Animation (von -> nach)
            String fromText = formatValue(flipFrom);
            String toText = formatValue(flipTo);

            float fontSize = Math.max(12f, diameter * fontRel);
            g2.setFont(getFont().deriveFont(Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();

            int textCenterX = winX + diameter / 2;
            int textCenterY = winY + diameter / 2;
            int baseY = textCenterY + (fm.getAscent() - fm.getDescent()) / 2;

            double p = flipProgress;
            Shape oldClip = g2.getClip();

            Shape topHalf = new Rectangle(winX, winY, diameter, diameter / 2);
            g2.setClip(topHalf);
            g2.clip(circle);
            drawCenteredText(g2, (p < 0.5 ? fromText : toText), textCenterX, baseY);

            Shape bottomHalf = new Rectangle(winX, winY + diameter / 2, diameter, diameter / 2);
            g2.setClip(bottomHalf);
            g2.clip(circle);

            Graphics2D gFlip = (Graphics2D) g2.create();
            try {
                double localP = (p < 0.5) ? (p / 0.5) : ((p - 0.5) / 0.5);
                double scaleY = (p < 0.5) ? (1.0 - localP) : (localP);

                int pivotY = midY;

                gFlip.translate(0, pivotY);
                gFlip.scale(1.0, Math.max(0.02, scaleY));
                gFlip.translate(0, -pivotY);

                float shade = (float) (0.25 + 0.35 * (1.0 - Math.abs(0.5 - p) * 2.0));
                gFlip.setColor(new Color(0, 0, 0, (int) (255 * shade)));
                gFlip.fill(circle);

                drawCenteredText(gFlip, (p < 0.5 ? fromText : toText), textCenterX, baseY);
            } finally {
                gFlip.dispose();
            }

            g2.setClip(oldClip);
        } finally {
            g2.dispose();
        }
    }

    private String formatValue(int v) {
        int current = display.getValue();
        if (current >= 0 && current <= 31) return String.format("%02d", v);
        return String.valueOf(v);
    }

    private void drawCenteredText(Graphics2D g2, String text, int centerX, int baseY) {
        FontMetrics fm = g2.getFontMetrics();
        int x = centerX - fm.stringWidth(text) / 2;

        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(text, x + 2, baseY + 2);

        g2.setColor(Color.WHITE);
        g2.drawString(text, x, baseY);
    }
}