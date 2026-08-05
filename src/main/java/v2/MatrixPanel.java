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
    private double targetAngleX = angleX;
    private double targetAngleY = angleY;
    private final Timer cameraTimer;
    private boolean thirdPersonCamera;
    private double cameraFocusX;
    private double cameraFocusY;
    private double cameraFocusZ;
    private double targetFocusX;
    private double targetFocusY;
    private double targetFocusZ;
    private double headingAngle = Math.PI / 2.0;
    private double targetHeadingAngle = headingAngle;
    private double thirdPersonMovementFactor = 0.22;

    private int lastMouseX;
    private int lastMouseY;
    private boolean dragged;

    public MatrixPanel() {
        setBackground(new Color(245, 247, 250));

        cameraTimer = new Timer(16, e -> animateCamera());

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                stopCameraFollow();
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

    public void followPath(Point3D from, Point3D to) {
        thirdPersonCamera = false;
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        int dz = to.z() - from.z();

        if (dx != 0 || dz != 0) {
            double desiredYaw = Math.atan2(dz, dx) + Math.PI / 4.0;
            targetAngleY = angleY + shortestAngleDifference(angleY, desiredYaw);
        }

        double verticalDirection = Math.atan2(dy, Math.max(1.0, Math.hypot(dx, dz)));
        targetAngleX = Math.clamp(-0.65 + verticalDirection * 0.35, -1.25, -0.15);

        if (!cameraTimer.isRunning()) {
            cameraTimer.start();
        }
    }

    public void setThirdPersonCamera(
            Point3D current,
            Point3D next,
            boolean animateMovement,
            int movementDurationMs
    ) {
        if (movementDurationMs <= 0) {
            animateMovement = false;
        } else {
            thirdPersonMovementFactor = 1.0 - Math.pow(0.002, 16.0 / movementDurationMs);
        }

        if (!thirdPersonCamera || !animateMovement) {
            cameraFocusX = current.x();
            cameraFocusY = current.y();
            cameraFocusZ = current.z();
        }

        thirdPersonCamera = true;
        targetFocusX = current.x();
        targetFocusY = current.y();
        targetFocusZ = current.z();

        if (next != null) {
            int dx = next.x() - current.x();
            int dz = next.z() - current.z();
            if (dx != 0 || dz != 0) {
                double desiredHeading = Math.atan2(dz, dx);
                targetHeadingAngle = headingAngle + shortestAngleDifference(headingAngle, desiredHeading);
                if (!animateMovement) {
                    headingAngle = targetHeadingAngle;
                }
            }
        }

        if (animateMovement && !cameraTimer.isRunning()) {
            cameraTimer.start();
        }
        repaint();
    }

    public void clearThirdPersonCamera() {
        thirdPersonCamera = false;
        stopCameraFollow();
        repaint();
    }

    public void stopCameraFollow() {
        cameraTimer.stop();
        targetAngleX = angleX;
        targetAngleY = angleY;
        targetFocusX = cameraFocusX;
        targetFocusY = cameraFocusY;
        targetFocusZ = cameraFocusZ;
        targetHeadingAngle = headingAngle;
    }

    private void animateCamera() {
        if (thirdPersonCamera) {
            animateThirdPersonCamera();
            return;
        }

        double xDifference = targetAngleX - angleX;
        double yDifference = targetAngleY - angleY;

        angleX += xDifference * 0.18;
        angleY += yDifference * 0.18;
        repaint();

        if (Math.abs(xDifference) < 0.002 && Math.abs(yDifference) < 0.002) {
            angleX = targetAngleX;
            angleY = targetAngleY;
            cameraTimer.stop();
        }
    }

    private void animateThirdPersonCamera() {
        double xDifference = targetFocusX - cameraFocusX;
        double yDifference = targetFocusY - cameraFocusY;
        double zDifference = targetFocusZ - cameraFocusZ;
        double headingDifference = targetHeadingAngle - headingAngle;

        cameraFocusX += xDifference * thirdPersonMovementFactor;
        cameraFocusY += yDifference * thirdPersonMovementFactor;
        cameraFocusZ += zDifference * thirdPersonMovementFactor;
        headingAngle += headingDifference * thirdPersonMovementFactor;
        repaint();

        if (Math.abs(xDifference) < 0.002
                && Math.abs(yDifference) < 0.002
                && Math.abs(zDifference) < 0.002
                && Math.abs(headingDifference) < 0.002) {
            cameraFocusX = targetFocusX;
            cameraFocusY = targetFocusY;
            cameraFocusZ = targetFocusZ;
            headingAngle = targetHeadingAngle;
            cameraTimer.stop();
        }
    }

    private double shortestAngleDifference(double from, double to) {
        return Math.atan2(Math.sin(to - from), Math.cos(to - from));
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
        } else if (thirdPersonCamera) {
            renderer3D.drawThirdPerson(
                    g2,
                    data,
                    visualScale,
                    cameraFocusX,
                    cameraFocusY,
                    cameraFocusZ,
                    Math.cos(headingAngle),
                    Math.sin(headingAngle),
                    getWidth(),
                    getHeight()
            );
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
