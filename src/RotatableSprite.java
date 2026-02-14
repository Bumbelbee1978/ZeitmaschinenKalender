import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class RotatableSprite extends JComponent {
    private final BufferedImage image;
    private double angleRadians;

    public RotatableSprite(BufferedImage image) {
        this.image = image;
        setOpaque(false);
    }

    public double getAngleRadians() {
        return angleRadians;
    }

    public void setAngleRadians(double angleRadians) {
        this.angleRadians = angleRadians;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) return;

        int cw = getWidth();
        int ch = getHeight();
        if (cw <= 0 || ch <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double cx = cw / 2.0;
            double cy = ch / 2.0;

            double sx = cw / (double) image.getWidth();
            double sy = ch / (double) image.getHeight();

            AffineTransform at = new AffineTransform();
            at.translate(cx, cy);
            at.rotate(angleRadians);
            at.scale(sx, sy);
            at.translate(-image.getWidth() / 2.0, -image.getHeight() / 2.0);

            g2.drawImage(image, at, null);
        } finally {
            g2.dispose();
        }
    }
}