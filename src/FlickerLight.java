import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

public class FlickerLight extends JComponent {
    private final Color baseColor;
    private float intensity = 0.0f; // 0..1
    private Timer timer;

    // Neu: Auto-Stop nach einer festen Zeit
    private Timer autoStopTimer;
    private static final int AUTO_STOP_MS = 2_000;

    public FlickerLight(Color baseColor) {
        this.baseColor = baseColor;
        setOpaque(false);
        setVisible(false);
    }

    public void startFlicker() {
        stopFlicker();
        setVisible(true);

        timer = new Timer(33, e -> {
            float r = (float) ThreadLocalRandom.current().nextDouble();
            intensity = 0.20f + 0.80f * (r * r); // leicht "hell-lastig"
            repaint();
        });
        timer.start();

        // Neu: nach 20 Sekunden automatisch stoppen
        autoStopTimer = new Timer(AUTO_STOP_MS, e -> stopFlicker());
        autoStopTimer.setRepeats(false);
        autoStopTimer.start();
    }

    public void stopFlicker() {
        if (autoStopTimer != null) {
            autoStopTimer.stop();
            autoStopTimer = null;
        }
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        intensity = 0.0f;
        setVisible(false);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int d = Math.min(w, h);
        int x = (w - d) / 2;
        int y = (h - d) / 2;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int glow = (int) (d * 0.75);
            int gx = x - glow / 2;
            int gy = y - glow / 2;
            int gd = d + glow;

            int alphaGlow = (int) (110 * intensity);
            g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alphaGlow));
            g2.fillOval(gx, gy, gd, gd);

            int alphaCore = (int) (220 * intensity);
            g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alphaCore));
            g2.fillOval(x, y, d, d);

            g2.setColor(new Color(255, 255, 255, (int) (130 * intensity)));
            g2.fillOval(x + d / 5, y + d / 5, d / 4, d / 4);
        } finally {
            g2.dispose();
        }
    }
}