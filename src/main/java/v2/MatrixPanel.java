package v2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class MatrixPanel extends JPanel {

    private RenderData data;
    private Consumer<Point3D> clickHandler;
    private IntConsumer wheelHandler;

    private final Renderer2D renderer2D = new Renderer2D();
    private final Renderer3D renderer3D = new Renderer3D();

    private double visualScale = 1.0;

    private double angleX = -0.65;
    private double angleY = 0.75;

    private int lastMouseX;
    private int lastMouseY;
    private boolean dragged;

    public MatrixPanel() {
        setBackground(new Color(245, 247, 250));

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                dragged = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;

                angleY += dx * 0.01;
                angleX += dy * 0.01;

                lastMouseX = e.getX();
                lastMouseY = e.getY();
                dragged = true;

                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!dragged && clickHandler != null) {
                    Point3D point = pickNode(e.getX(), e.getY());
                    if (point != null) {
                        clickHandler.accept(point);
                    }
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        addMouseWheelListener(e -> {
            if (wheelHandler != null) {
                wheelHandler.accept(e.getWheelRotation());
            }
        });
    }

    public void setClickHandler(Consumer<Point3D> clickHandler) {
        this.clickHandler = clickHandler;
    }

    public void setWheelHandler(IntConsumer wheelHandler) {
        this.wheelHandler = wheelHandler;
    }

    public void setData(RenderData data) {
        this.data = data;
        repaint();
    }

    public void setScale(double visualScale) {
        this.visualScale = visualScale;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (data == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        if (data.grid.depth == 1) {
            renderer2D.draw(g2, data, visualScale);
        } else {
            renderer3D.draw(g2, data, visualScale, angleX, angleY, getWidth(), getHeight());
        }
    }

    private Point3D pickNode(int mouseX, int mouseY) {
        if (data == null || data.pathType != PathType.INTERACTIVE) {
            return null;
        }

        if (data.grid.depth == 1) {
            return pickNode2D(mouseX, mouseY);
        }

        return pickNode3D(mouseX, mouseY);
    }

    private Point3D pickNode2D(int mouseX, int mouseY) {
        int cellSize = Math.max(8, (int) (45 * visualScale));
        int startX = 40;
        int startY = 40;

        for (Point3D p : data.interactiveFrontier) {
            int drawX = startX + p.x() * cellSize;
            int drawY = startY + p.y() * cellSize;

            if (mouseX >= drawX
                    && mouseX <= drawX + cellSize
                    && mouseY >= drawY
                    && mouseY <= drawY + cellSize) {
                return p;
            }
        }

        return null;
    }

    private Point3D pickNode3D(int mouseX, int mouseY) {
        Projection3D projection = new Projection3D(
                data.grid,
                visualScale,
                angleX,
                angleY,
                getWidth(),
                getHeight()
        );

        Point3D bestPoint = null;
        double bestDepth = Double.POSITIVE_INFINITY;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (Point3D p : data.interactiveFrontier) {
            ProjectedPoint projected = projection.project(p.x(), p.y(), p.z());
            int size = Math.max(3, (int) (12 * projected.scale() * visualScale));
            double radius = size / 2.0;
            double dx = mouseX - projected.screenX();
            double dy = mouseY - projected.screenY();
            double distance = Math.hypot(dx, dy);

            if (distance <= radius + 2) {
                if (projected.depth() < bestDepth
                        || (projected.depth() == bestDepth && distance < bestDistance)) {
                    bestDepth = projected.depth();
                    bestDistance = distance;
                    bestPoint = p;
                }
            }
        }

        return bestPoint;
    }
}
