import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

public class SteamCloud extends JComponent {
    private float alpha = 0f;
    private float yOffset = 0f;
    private float driftX = 0f;

    private Timer timer;

    // Neu: wenn true, wird nur noch ausgeblendet und dann gestoppt
    private boolean finishing = false;

    public SteamCloud() {
        setOpaque(false);
        setVisible(false);
    }

    public void startSteam() {
        stopSteam();
        setVisible(true);

        alpha = 0.20f;
        yOffset = 0f;
        driftX = 0f;
        finishing = false;

        timer = new Timer(33, e -> {
            // Bewegung
            yOffset -= 0.55f;
            driftX += (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.35f;

            // Einblenden am Anfang, aber nicht über 1.0
            if (!finishing) {
                alpha = Math.min(1f, alpha + 0.015f);
            } else {
                // Ausfaden am Ende
                alpha = Math.max(0f, alpha - 0.04f);
                if (alpha <= 0.001f) {
                    stopSteam();
                    return;
                }
            }

            // Neu: Sobald die Wolke weit genug oben ist -> finishing starten (kein Reset mehr)
            if (!finishing && yOffset < -getHeight() * 0.9f) {
                finishing = true;
            }

            repaint();
        });
        timer.start();
    }

    public void stopSteam() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        finishing = false;
        setVisible(false);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.SrcOver.derive(0.55f * Math.max(0f, Math.min(1f, alpha))));

            int baseY = (int) (h * 0.70f + yOffset);
            int baseX = (int) (w * 0.50f + driftX);

            for (int i = 0; i < 6; i++) {
                float k = 1f - (i / 6f);

                int puffW = (int) (w * (0.55 + 0.35 * k));
                int puffH = (int) (h * (0.18 + 0.20 * k));

                int x = baseX - puffW / 2 + (int) (Math.sin((yOffset + i * 11) * 0.06) * 8);
                int y = baseY - (int) (i * h * 0.12);

                g2.setColor(new Color(225, 225, 225, (int) (255 * (0.18 + 0.10 * k))));
                g2.fillOval(x, y, puffW, puffH);
            }
        } finally {
            g2.dispose();
        }
    }
}