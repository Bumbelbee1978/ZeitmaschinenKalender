import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class LeverControl extends JComponent {
    private final BufferedImage image;

    private int dragMinY = 0;
    private int dragMaxY = 0;

    private boolean dragging;
    private int grabOffsetY;

    private Runnable onLatchedBottom;
    private boolean latchedAtBottomFired;

    // Wie weit man sich vom unteren Anschlag entfernen muss, um erneut auslösen zu können
    private static final int RELATCH_DEADBAND_PX = 12;

    public LeverControl(BufferedImage image) {
        this.image = image;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragging = true;
                grabOffsetY = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
                snapToEnd();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging) return;

                Container parent = getParent();
                if (parent == null) return;

                int newY = getY() + (e.getY() - grabOffsetY);
                newY = Math.max(dragMinY, Math.min(dragMaxY, newY));

                setLocation(getX(), newY);
                parent.repaint();

                // Neu: sobald man ein Stück weg vom unteren Anschlag ist, darf erneut ausgelöst werden
                if (newY <= dragMaxY - RELATCH_DEADBAND_PX) {
                    latchedAtBottomFired = false;
                }

                // feuert beim Erreichen der unteren Position
                if (newY == dragMaxY && !latchedAtBottomFired) {
                    latchedAtBottomFired = true;
                    if (onLatchedBottom != null) {
                        try {
                            onLatchedBottom.run();
                        } catch (Throwable ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setOnLatchedBottom(Runnable onLatchedBottom) {
        this.onLatchedBottom = onLatchedBottom;
    }

    public void setDragYRange(int minY, int maxY) {
        this.dragMinY = Math.min(minY, maxY);
        this.dragMaxY = Math.max(minY, maxY);

        int clamped = Math.max(dragMinY, Math.min(dragMaxY, getY()));
        if (clamped != getY()) setLocation(getX(), clamped);
    }

    private void snapToEnd() {
        Container parent = getParent();
        if (parent == null) return;

        int y = getY();
        int mid = dragMinY + (dragMaxY - dragMinY) / 2;

        int targetY = (y <= mid) ? dragMinY : dragMaxY;

        setLocation(getX(), targetY);
        parent.repaint();

        if (targetY == dragMaxY && !latchedAtBottomFired) {
            latchedAtBottomFired = true;
            if (onLatchedBottom != null) {
                try {
                    onLatchedBottom.run();
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (targetY == dragMinY) {
            latchedAtBottomFired = false;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        } finally {
            g2.dispose();
        }
    }
}