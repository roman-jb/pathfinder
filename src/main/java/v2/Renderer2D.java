package v2;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

        if (data.pathType == PathType.INTERACTIVE) {
            drawInteractive(g2, data, pathSet, cellSize, startX, startY, scale);
            return;
        }

        for (int y = 0; y < grid.height; y++) {
            for (int x = 0; x < grid.width; x++) {
                Point3D p = new Point3D(x, y, 0);

                if (data.hideUnusedNodes && !pathSet.contains(p)) {
                    continue;
                }

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

    private void drawInteractive(
            Graphics2D g2,
            RenderData data,
            Set<Point3D> pathSet,
            int cellSize,
            int startX,
            int startY,
            double scale
    ) {
        Grid grid = data.grid;

        g2.setStroke(new BasicStroke(Math.max(1f, (float) (2f * scale))));

        drawInteractiveLinks(g2, data, cellSize, startX, startY);

        for (int y = 0; y < grid.height; y++) {
            for (int x = 0; x < grid.width; x++) {
                Point3D p = new Point3D(x, y, 0);

                if (!RenderUtils.isInteractiveVisible(data, p)) {
                    continue;
                }

                int drawX = startX + x * cellSize;
                int drawY = startY + y * cellSize;

                Color color = RenderUtils.getCellColor(data, p, pathSet);
                if (color == null) {
                    continue;
                }

                if (p.equals(data.start) || pathSet.contains(p)) {
                    g2.setColor(color);
                    g2.fillRect(drawX, drawY, cellSize, cellSize);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(drawX, drawY, cellSize, cellSize);
                } else {
                    drawInteractiveSphere(g2, drawX, drawY, cellSize, color);
                }
            }
        }
    }

    private void drawInteractiveSphere(Graphics2D g2, int drawX, int drawY, int cellSize, Color color) {
        int inset = Math.max(4, cellSize / 6);
        int size = cellSize - inset * 2;

        g2.setColor(color);
        g2.fillOval(drawX + inset, drawY + inset, size, size);

        g2.setColor(new Color(80, 80, 80, 160));
        g2.drawOval(drawX + inset, drawY + inset, size, size);

        int highlightSize = Math.max(2, size / 3);
        g2.setColor(new Color(255, 255, 255, 110));
        g2.fillOval(drawX + inset + size / 5, drawY + inset + size / 5, highlightSize, highlightSize);
    }

    private void drawInteractiveLinks(
            Graphics2D g2,
            RenderData data,
            int cellSize,
            int startX,
            int startY
    ) {
        List<Point3D> path = new ArrayList<>(data.path);

        if (path.size() >= 2) {
            g2.setColor(new Color(75, 145, 235));
            for (int i = 1; i < path.size(); i++) {
                Point3D a = path.get(i - 1);
                Point3D b = path.get(i);
                g2.drawLine(
                        startX + a.x * cellSize + cellSize / 2,
                        startY + a.y * cellSize + cellSize / 2,
                        startX + b.x * cellSize + cellSize / 2,
                        startY + b.y * cellSize + cellSize / 2
                );
            }
        }

        if (data.interactiveSelected != null && !path.isEmpty()) {
            Point3D lastConfirmed = path.getLast();
            Point3D selected = data.interactiveSelected;

            g2.setColor(new Color(70, 190, 90));
            g2.drawLine(
                    startX + lastConfirmed.x * cellSize + cellSize / 2,
                    startY + lastConfirmed.y * cellSize + cellSize / 2,
                    startX + selected.x * cellSize + cellSize / 2,
                    startY + selected.y * cellSize + cellSize / 2
            );
        }
    }
}
