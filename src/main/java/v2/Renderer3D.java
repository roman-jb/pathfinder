package v2;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Renderer3D {

    public void draw(
            Graphics2D g2,
            RenderData data,
            double scale,
            double angleX,
            double angleY,
            int panelWidth,
            int panelHeight
    ) {
        drawLegend(g2, data);

        Set<Point3D> pathSet = new HashSet<>(data.path);

        if (data.pathType == PathType.INTERACTIVE) {
            drawInteractive(g2, data, scale, angleX, angleY, panelWidth, panelHeight, pathSet);
            return;
        }

        List<Sphere> spheres = new ArrayList<>();
        List<Cube> pathCubes = new ArrayList<>();

        Grid grid = data.grid;

        Projection3D projection = new Projection3D(
                grid,
                scale,
                angleX,
                angleY,
                panelWidth,
                panelHeight
        );

        for (int z = 0; z < grid.depth; z++) {
            for (int y = 0; y < grid.height; y++) {
                for (int x = 0; x < grid.width; x++) {
                    Point3D p = new Point3D(x, y, z);

                    if (pathSet.contains(p)) {
                        pathCubes.add(createCube(p, projection, scale));
                    } else if (!data.hideUnusedNodes) {
                        spheres.add(new Sphere(
                                p,
                                projection.project(p.x, p.y, p.z),
                                RenderUtils.getCellColor(data, p, pathSet),
                                RenderUtils.getCellText(data, p)
                        ));
                    }
                }
            }
        }

        spheres.sort(Comparator.comparingDouble(s -> s.projected.depth));
        pathCubes.sort(Comparator.comparingDouble(c -> c.depth));

        for (Sphere sphere : spheres) {
            drawSphere(g2, sphere, scale);
        }

        drawPathLines(g2, data.path, projection, scale);

        for (Cube cube : pathCubes) {
            drawCube(g2, data, cube, pathSet, scale, projection);
        }
    }

    private void drawInteractive(
            Graphics2D g2,
            RenderData data,
            double scale,
            double angleX,
            double angleY,
            int panelWidth,
            int panelHeight,
            Set<Point3D> pathSet
    ) {
        Grid grid = data.grid;
        Projection3D projection = new Projection3D(
                grid,
                scale,
                angleX,
                angleY,
                panelWidth,
                panelHeight
        );

        List<Sphere> spheres = new ArrayList<>();
        List<Cube> pathCubes = new ArrayList<>();

        for (int z = 0; z < grid.depth; z++) {
            for (int y = 0; y < grid.height; y++) {
                for (int x = 0; x < grid.width; x++) {
                    Point3D p = new Point3D(x, y, z);

                    if (RenderUtils.isInteractiveVisible(data, p)) {
                        if (pathSet.contains(p)) {
                            pathCubes.add(createCube(p, projection, scale));
                        } else {
                            spheres.add(new Sphere(
                                    p,
                                    projection.project(p.x, p.y, p.z),
                                    RenderUtils.getCellColor(data, p, pathSet),
                                    RenderUtils.getCellText(data, p)
                            ));
                        }
                    }
                }
            }
        }

        spheres.sort(Comparator.comparingDouble(s -> s.projected.depth));
        pathCubes.sort(Comparator.comparingDouble(c -> c.depth));

        for (Sphere sphere : spheres) {
            drawSphere(g2, sphere, scale);
        }

        drawInteractiveLinks(g2, data, projection, scale);

        for (Cube cube : pathCubes) {
            drawCube(g2, data, cube, pathSet, scale, projection);
        }
    }

    private void drawLegend(Graphics2D g2, RenderData data) {
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(Color.BLACK);
        g2.drawString("3D mode: drag mouse to rotate", 20, 25);
        if (data.pathType == PathType.INTERACTIVE) {
            g2.drawString("Click gray spheres, then click again to confirm", 20, 45);
            g2.drawString("Green = active step, Blue = confirmed path", 20, 65);
        } else {
            g2.drawString(
                    data.hideUnusedNodes
                            ? "Unused cells hidden"
                            : "Small gray spheres = unused cells",
                    20,
                    45
            );
            g2.drawString("Green = Start, Red = End, Blue = Path", 20, 65);
        }
    }

    private void drawSphere(Graphics2D g2, Sphere sphere, double scale) {
        ProjectedPoint projected = sphere.projected;

        int baseSize = Math.max(3, (int) (12 * projected.scale * scale));
        int size = baseSize;
        Font font;
        FontMetrics fm = null;

        if (sphere.label != null && !sphere.label.isBlank()) {
            font = new Font("Arial", Font.BOLD, Math.max(8, (int) (14 * projected.scale * scale)));
            g2.setFont(font);
            fm = g2.getFontMetrics();
            int labelWidth = fm.stringWidth(sphere.label);
            int labelHeight = fm.getHeight();
            size = Math.max(baseSize, Math.max(labelWidth, labelHeight) + 12);
        }

        int x = projected.screenX - size / 2;
        int y = projected.screenY - size / 2;

        Color fill = sphere.color != null ? sphere.color : new Color(155, 155, 155, 135);
        Color outline = sphere.color != null
                ? RenderUtils.darken(sphere.color, 0.72)
                : new Color(80, 80, 80, 120);

        g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), fill.getAlpha()));
        g2.fillOval(x, y, size, size);

        g2.setColor(new Color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha()));
        g2.drawOval(x, y, size, size);

        int highlightSize = Math.max(2, size / 3);
        g2.setColor(new Color(255, 255, 255, 120));
        g2.fillOval(x + size / 5, y + size / 5, highlightSize, highlightSize);

        if (sphere.label != null && !sphere.label.isBlank() && fm != null) {
            g2.setColor(Color.BLACK);
            int textX = projected.screenX - fm.stringWidth(sphere.label) / 2;
            int textY = projected.screenY + fm.getAscent() / 2;
            g2.drawString(sphere.label, textX, textY);
        }
    }

    private Cube createCube(Point3D p, Projection3D projection, double scale) {
        double size = Math.max(0.25, 0.72 * scale);

        double[][] corners = getDoubles(p, size);

        ProjectedPoint[] projected = new ProjectedPoint[8];
        double avgDepth = 0;

        for (int i = 0; i < corners.length; i++) {
            projected[i] = projection.project(corners[i][0], corners[i][1], corners[i][2]);
            avgDepth += projected[i].depth;
        }

        avgDepth /= 8.0;

        return new Cube(p, projected, avgDepth);
    }

    private static double[][] getDoubles(Point3D p, double size) {
        double x = p.x;
        double y = p.y;
        double z = p.z;

        return new double[][]{
                {x - size / 2, y - size / 2, z - size / 2},
                {x + size / 2, y - size / 2, z - size / 2},
                {x + size / 2, y + size / 2, z - size / 2},
                {x - size / 2, y + size / 2, z - size / 2},

                {x - size / 2, y - size / 2, z + size / 2},
                {x + size / 2, y - size / 2, z + size / 2},
                {x + size / 2, y + size / 2, z + size / 2},
                {x - size / 2, y + size / 2, z + size / 2}
        };
    }

    private void drawCube(
            Graphics2D g2,
            RenderData data,
            Cube cube,
            Set<Point3D> pathSet,
            double scale,
            Projection3D projection
    ) {
        Color base = RenderUtils.getCellColor(data, cube.point, pathSet);

        assert base != null;
        drawFace(g2, cube, new int[]{0, 1, 2, 3}, RenderUtils.darken(base, 0.78));
        drawFace(g2, cube, new int[]{4, 5, 6, 7}, RenderUtils.brighten(base, 1.05));
        drawFace(g2, cube, new int[]{0, 1, 5, 4}, RenderUtils.brighten(base, 1.15));
        drawFace(g2, cube, new int[]{2, 3, 7, 6}, RenderUtils.darken(base, 0.7));
        drawFace(g2, cube, new int[]{1, 2, 6, 5}, base);
        drawFace(g2, cube, new int[]{0, 3, 7, 4}, RenderUtils.darken(base, 0.88));

        drawCubeEdges(g2, cube, scale);

        if (scale >= 0.45) {
            draw3DLabel(g2, data, cube, scale, projection);
        }
    }

    private void drawFace(Graphics2D g2, Cube cube, int[] indexes, Color color) {
        Polygon polygon = new Polygon();

        for (int index : indexes) {
            polygon.addPoint(
                    cube.points[index].screenX,
                    cube.points[index].screenY
            );
        }

        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 220));
        g2.fillPolygon(polygon);

        g2.setColor(new Color(30, 30, 30, 90));
        g2.drawPolygon(polygon);
    }

    private void drawCubeEdges(Graphics2D g2, Cube cube, double scale) {
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };

        g2.setStroke(new BasicStroke(Math.max(0.5f, (float) scale)));
        g2.setColor(new Color(20, 20, 20, 100));

        for (int[] edge : edges) {
            ProjectedPoint a = cube.points[edge[0]];
            ProjectedPoint b = cube.points[edge[1]];
            g2.drawLine(a.screenX, a.screenY, b.screenX, b.screenY);
        }
    }

    private void draw3DLabel(
            Graphics2D g2,
            RenderData data,
            Cube cube,
            double scale,
            Projection3D projection
    ) {
        ProjectedPoint center = projection.project(
                cube.point.x,
                cube.point.y,
                cube.point.z
        );

        String text = RenderUtils.getCellText(data, cube.point);

        g2.setFont(new Font("Arial", Font.BOLD, Math.max(8, (int) (16 * scale))));
        FontMetrics fm = g2.getFontMetrics();

        int x = center.screenX - fm.stringWidth(text) / 2;
        int y = center.screenY + fm.getAscent() / 2;

        g2.setColor(Color.BLACK);
        g2.drawString(text, x, y);
    }

    private void drawInteractiveLinks(Graphics2D g2, RenderData data, Projection3D projection, double scale) {
        List<Point3D> path = data.path;

        if (path.size() >= 2) {
            g2.setStroke(new BasicStroke(
                    Math.max(2f, (float) (6f * scale)),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            ));
            g2.setColor(new Color(75, 145, 235, 190));

            for (int i = 1; i < path.size(); i++) {
                Point3D aPoint = path.get(i - 1);
                Point3D bPoint = path.get(i);

                ProjectedPoint a = projection.project(aPoint.x, aPoint.y, aPoint.z);
                ProjectedPoint b = projection.project(bPoint.x, bPoint.y, bPoint.z);

                g2.drawLine(a.screenX, a.screenY, b.screenX, b.screenY);
            }
        }

        if (data.interactiveSelected != null && !path.isEmpty()) {
            Point3D lastConfirmed = path.getLast();
            Point3D selected = data.interactiveSelected;

            g2.setStroke(new BasicStroke(
                    Math.max(2f, (float) (6f * scale)),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            ));
            g2.setColor(new Color(70, 190, 90, 190));

            ProjectedPoint a = projection.project(lastConfirmed.x, lastConfirmed.y, lastConfirmed.z);
            ProjectedPoint b = projection.project(selected.x, selected.y, selected.z);
            g2.drawLine(a.screenX, a.screenY, b.screenX, b.screenY);
        }

        g2.setStroke(new BasicStroke(1f));
    }

    private void drawPathLines(
            Graphics2D g2,
            List<Point3D> path,
            Projection3D projection,
            double scale
    ) {
        if (path.size() < 2) return;

        g2.setStroke(new BasicStroke(
                Math.max(2f, (float) (6f * scale)),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
        ));

        g2.setColor(new Color(20, 80, 220, 190));

        for (int i = 1; i < path.size(); i++) {
            Point3D aPoint = path.get(i - 1);
            Point3D bPoint = path.get(i);

            ProjectedPoint a = projection.project(aPoint.x, aPoint.y, aPoint.z);
            ProjectedPoint b = projection.project(bPoint.x, bPoint.y, bPoint.z);

            g2.drawLine(a.screenX, a.screenY, b.screenX, b.screenY);
        }

        g2.setStroke(new BasicStroke(1f));
    }
}
