package v2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MatrixPanel extends JPanel {

    private RenderData data;

    private final Renderer2D renderer2D = new Renderer2D();
    private final Renderer3D renderer3D = new Renderer3D();

    private double visualScale = 1.0;

    private double angleX = -0.65;
    private double angleY = 0.75;

    private int lastMouseX;
    private int lastMouseY;

    public MatrixPanel() {
        setBackground(new Color(245, 247, 250));

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;

                angleY += dx * 0.01;
                angleX += dy * 0.01;

                lastMouseX = e.getX();
                lastMouseY = e.getY();

                repaint();
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
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
}
