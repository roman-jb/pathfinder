package v2;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class Renderer3D {

    private static final Font LEGEND_FONT = new Font("Arial", Font.BOLD, 14);
    private static final Color UNUSED_NODE_COLOR = new Color(120, 120, 120, 150);
    private static final Color UNUSED_NODE_OUTLINE = new Color(80, 80, 80, 120);
    private static final Color SPHERE_HIGHLIGHT = new Color(255, 255, 255, 120);
    private static final Color FACE_OUTLINE = new Color(30, 30, 30, 90);
    private static final Color EDGE_COLOR = new Color(20, 20, 20, 100);
    private static final Color PATH_LINE_COLOR = new Color(20, 80, 220, 190);
    private static final Color INTERACTIVE_LINE_COLOR = new Color(75, 145, 235, 190);
    private static final Color INTERACTIVE_LINK_COLOR = new Color(70, 190, 90, 190);
    private static final int[][] CUBE_FACES = {
            {0, 1, 2, 3},
            {4, 5, 6, 7},
            {0, 1, 5, 4},
            {2, 3, 7, 6},
            {1, 2, 6, 5},
            {0, 3, 7, 4}
    };
    private static final int[][] CUBE_EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

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

        Set<Point3D> pathSet = data.pathSet;

        if (data.pathType == PathType.INTERACTIVE) {
            drawInteractive(g2, data, scale, angleX, angleY, panelWidth, panelHeight, pathSet);
            return;
        }

        List<Sphere> spheres = new ArrayList<>();
        List<Cube> unusedCubes = new ArrayList<>();
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
                        pathCubes.add(createCube(
                                p,
                                projection,
                                1.0
                        ));
                    } else if (!data.hideUnusedNodes) {
                        if (RenderUtils.isSurfaceNode(grid, p)) {
                            unusedCubes.add(createCube(
                                    p,
                                    projection,
                                    0.5
                            ));
                        } else {
                            spheres.add(new Sphere(
                                    p,
                                    projection.project(p.x, p.y, p.z),
                                    UNUSED_NODE_COLOR,
                                    null,
                                    0.5
                            ));
                        }
                    }
                }
            }
        }

        spheres.sort(Comparator.comparingDouble(s -> s.projected.depth));
        unusedCubes.sort(Comparator.comparingDouble(c -> c.depth));
        pathCubes.sort(Comparator.comparingDouble(c -> c.depth));

        for (Sphere sphere : spheres) {
            drawSphere(g2, sphere, scale);
        }

        drawPathLines(g2, data.path, projection, scale);

        for (Cube cube : unusedCubes) {
            drawCube(g2, cube, UNUSED_NODE_COLOR, null, scale, projection);
        }

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
                        if (pathSet.contains(p) || RenderUtils.isInteractiveBox(data, p)) {
                            pathCubes.add(createCube(
                                    p,
                                    projection,
                                    RenderUtils.getInteractiveSizeFactor(data, p)
                            ));
                        } else if (RenderUtils.isInteractiveSphere(data, p)) {
                            spheres.add(new Sphere(
                                    p,
                                    projection.project(p.x, p.y, p.z),
                                    RenderUtils.getCellColor(data, p, pathSet),
                                    RenderUtils.getCellText(data, p),
                                    RenderUtils.getInteractiveSizeFactor(data, p)
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
        g2.setFont(LEGEND_FONT);
        g2.setColor(Color.BLACK);
        g2.drawString("3D mode: drag mouse to rotate", 20, 25);
        if (data.pathType == PathType.INTERACTIVE) {
            g2.drawString("Surface nodes are boxes; interior nodes are spheres", 20, 45);
            g2.drawString("Green = selectable, Black = unused, Blue = confirmed path", 20, 65);
        } else {
            g2.drawString(
                    data.hideUnusedNodes
                            ? "Unused cells hidden"
                            : "Small gray cubes/spheres = unused cells",
                    20,
                    45
            );
            g2.drawString("Green = Start, Red = End, Blue = Path", 20, 65);
        }
    }

    private void drawSphere(Graphics2D g2, Sphere sphere, double scale) {
        ProjectedPoint projected = sphere.projected;

        int baseSize = Math.max(3, (int) (12 * projected.scale * scale * sphere.sizeFactor));
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
                : UNUSED_NODE_OUTLINE;

        g2.setColor(fill);
        g2.fillOval(x, y, size, size);

        g2.setColor(outline);
        g2.drawOval(x, y, size, size);

        int highlightSize = Math.max(2, size / 3);
        g2.setColor(SPHERE_HIGHLIGHT);
        g2.fillOval(x + size / 5, y + size / 5, highlightSize, highlightSize);

        if (sphere.label != null && !sphere.label.isBlank() && fm != null) {
            g2.setColor(Color.BLACK);
            int textX = projected.screenX - fm.stringWidth(sphere.label) / 2;
            int textY = projected.screenY + fm.getAscent() / 2;
            g2.drawString(sphere.label, textX, textY);
        }
    }

    private Cube createCube(Point3D p, Projection3D projection, double sizeFactor) {
        double size = Math.max(0.05, (12.0 / 62.0) * sizeFactor);
        double half = size / 2.0;
        double x = p.x;
        double y = p.y;
        double z = p.z;

        ProjectedPoint[] projected = new ProjectedPoint[8];
        double avgDepth = 0;

        projected[0] = projection.project(x - half, y - half, z - half);
        projected[1] = projection.project(x + half, y - half, z - half);
        projected[2] = projection.project(x + half, y + half, z - half);
        projected[3] = projection.project(x - half, y + half, z - half);
        projected[4] = projection.project(x - half, y - half, z + half);
        projected[5] = projection.project(x + half, y - half, z + half);
        projected[6] = projection.project(x + half, y + half, z + half);
        projected[7] = projection.project(x - half, y + half, z + half);

        for (ProjectedPoint point : projected) {
            avgDepth += point.depth;
        }

        avgDepth /= 8.0;

        return new Cube(p, projected, avgDepth, sizeFactor);
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
        String label = RenderUtils.getCellText(data, cube.point);
        drawCube(g2, cube, base, label, scale, projection);
    }

    private void drawCube(
            Graphics2D g2,
            Cube cube,
            Color base,
            String label,
            double scale,
            Projection3D projection
    ) {

        drawFace(g2, cube, CUBE_FACES[0], RenderUtils.darken(base, 0.78));
        drawFace(g2, cube, CUBE_FACES[1], RenderUtils.brighten(base, 1.05));
        drawFace(g2, cube, CUBE_FACES[2], RenderUtils.brighten(base, 1.15));
        drawFace(g2, cube, CUBE_FACES[3], RenderUtils.darken(base, 0.7));
        drawFace(g2, cube, CUBE_FACES[4], base);
        drawFace(g2, cube, CUBE_FACES[5], RenderUtils.darken(base, 0.88));

        drawCubeEdges(g2, cube, scale * cube.sizeFactor);

        if (scale >= 0.45 && label != null && !label.isBlank()) {
            draw3DLabel(g2, cube, label, scale, projection);
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

        g2.setColor(FACE_OUTLINE);
        g2.drawPolygon(polygon);
    }

    private void drawCubeEdges(Graphics2D g2, Cube cube, double scale) {
        g2.setStroke(new BasicStroke(Math.max(0.5f, (float) scale)));
        g2.setColor(EDGE_COLOR);

        for (int[] edge : CUBE_EDGES) {
            ProjectedPoint a = cube.points[edge[0]];
            ProjectedPoint b = cube.points[edge[1]];
            g2.drawLine(a.screenX, a.screenY, b.screenX, b.screenY);
        }
    }

    private void draw3DLabel(
            Graphics2D g2,
            Cube cube,
            String text,
            double scale,
            Projection3D projection
    ) {
        ProjectedPoint center = projection.project(
                cube.point.x,
                cube.point.y,
                cube.point.z
        );

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
            g2.setColor(INTERACTIVE_LINE_COLOR);

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
            g2.setColor(INTERACTIVE_LINK_COLOR);

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

        g2.setColor(PATH_LINE_COLOR);

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
