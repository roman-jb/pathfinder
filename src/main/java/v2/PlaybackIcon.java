package v2;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

enum PlaybackIcon implements Icon {
    PLAY, PAUSE, NEXT, PREVIOUS;

    private static final int SIZE = 18;

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = component.isEnabled()
                    ? component.getForeground()
                    : UIManager.getColor("Button.disabledText");
            g.setColor(color != null ? color : Color.GRAY);

            switch (this) {
                case PLAY -> g.fillPolygon(new int[]{4, 15, 4}, new int[]{2, 9, 16}, 3);
                case PAUSE -> {
                    g.fillRect(3, 2, 4, 14);
                    g.fillRect(11, 2, 4, 14);
                }
                case NEXT -> {
                    g.fillPolygon(new int[]{2, 12, 2}, new int[]{2, 9, 16}, 3);
                    g.fillRect(13, 2, 3, 14);
                }
                case PREVIOUS -> {
                    g.fillRect(2, 2, 3, 14);
                    g.fillPolygon(new int[]{16, 6, 16}, new int[]{2, 9, 16}, 3);
                }
            }
        } finally {
            g.dispose();
        }
    }
}
