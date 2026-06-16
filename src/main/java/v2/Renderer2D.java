package v2;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class Renderer2D {

    private static final int BASE_CELL_SIZE = 45;

    public void draw(Graphics2D g2, RenderData data, double scale) {
        Grid grid = data.grid;
        Set<Point3D> pathSet = new HashSet<>(data.path);

        int cellSize = Math.max(8, (int) (BASE_CELL_SIZE * scale));

        g2.setFont(new Font("Arial", Font.BOLD, Math.max(8, (int) (14 * scale))));

        int startX = 40;
        int startY = 40;

        for (int y = 0; y < grid.height; y++) {
            for (int x = 0; x < grid.width; x++) {
                Point3D p = new Point3D(x, y, 0);

                int drawX = startX + x * cellSize;
                int drawY = startY + y * cellSize;

                g2.setColor(RenderUtils.getCellColor(data, p, pathSet));
                g2.fillRect(drawX, drawY, cellSize, cellSize);

                g2.setColor(Color.BLACK);
                g2.drawRect(drawX, drawY, cellSize, cellSize);

                if (cellSize >= 18) {
                    String text = RenderUtils.getCellText(data, p);

                    FontMetrics fm = g2.getFontMetrics();
                    int textX = drawX + (cellSize - fm.stringWidth(text)) / 2;
                    int textY = drawY + ((cellSize - fm.getHeight()) / 2) + fm.getAscent();

                    g2.drawString(text, textX, textY);
                }
            }
        }
    }
}
