package v1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class PathfindingApp extends JFrame {

    private final JCheckBox mode3DCheckBox = new JCheckBox("3D mode");
    private final JComboBox<String> pathTypeBox = new JComboBox<>(new String[]{"Cheapest", "Shortest"});

    private final JTextField widthField = new JTextField("8", 4);
    private final JTextField heightField = new JTextField("8", 4);
    private final JTextField depthField = new JTextField("4", 4);

    private final JSlider scaleSlider = new JSlider(30, 150, 100);

    private final JButton rerollPointsButton = new JButton("New Start / End");

    private final JLabel pathLengthLabel = new JLabel("Length: -");
    private final JLabel pathCostLabel = new JLabel("Cost: -");

    private final MatrixPanel matrixPanel = new MatrixPanel();

    private Grid currentGrid;
    private Point3D currentStart;
    private Point3D currentEnd;

    public PathfindingApp() {
        super("Pathfinding 2D / 3D");

        JButton generateButton = new JButton("Generate Matrix");

        JPanel controls = new JPanel();

        controls.add(mode3DCheckBox);

        controls.add(new JLabel("Path type:"));
        controls.add(pathTypeBox);

        controls.add(new JLabel("Width X:"));
        controls.add(widthField);

        controls.add(new JLabel("Height Y:"));
        controls.add(heightField);

        controls.add(new JLabel("Depth Z:"));
        controls.add(depthField);

        controls.add(new JLabel("Scale:"));
        controls.add(scaleSlider);

        controls.add(generateButton);
        controls.add(rerollPointsButton);

        controls.add(pathLengthLabel);
        controls.add(pathCostLabel);

        rerollPointsButton.setEnabled(false);
        depthField.setEnabled(false);

        scaleSlider.setMajorTickSpacing(30);
        scaleSlider.setPaintTicks(true);

        add(controls, BorderLayout.NORTH);
        add(matrixPanel, BorderLayout.CENTER);

        generateButton.addActionListener(e -> generateMatrix());
        rerollPointsButton.addActionListener(e -> rerollStartEnd());

        mode3DCheckBox.addActionListener(e -> {
            depthField.setEnabled(mode3DCheckBox.isSelected());
        });

        pathTypeBox.addActionListener(e -> {
            if (currentGrid != null && currentStart != null && currentEnd != null) {
                rebuildPath();
            }
        });

        scaleSlider.addChangeListener(e -> {
            double scale = scaleSlider.getValue() / 100.0;
            matrixPanel.setScale(scale);
        });

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 850);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void generateMatrix() {
        boolean is3D = mode3DCheckBox.isSelected();

        int width = Integer.parseInt(widthField.getText());
        int height = Integer.parseInt(heightField.getText());
        int depth = is3D ? Integer.parseInt(depthField.getText()) : 1;

        currentGrid = new Grid(width, height, depth);
        currentGrid.randomizeWeights();

        rerollPointsButton.setEnabled(true);
        rerollStartEnd();
    }

    private void rerollStartEnd() {
        if (currentGrid == null) return;

        currentStart = currentGrid.randomPoint();

        do {
            currentEnd = currentGrid.randomPoint();
        } while (currentEnd.equals(currentStart));

        rebuildPath();
    }

    private void rebuildPath() {
        boolean cheapest = pathTypeBox.getSelectedItem().equals("Cheapest");

        List<Point3D> path = Pathfinder.findPath(
                currentGrid,
                currentStart,
                currentEnd,
                cheapest
        );

        int pathLength = Math.max(0, path.size() - 1);
        int pathCost = currentGrid.calculatePathCost(path);

        pathLengthLabel.setText("Length: " + pathLength);
        pathCostLabel.setText("Cost: " + pathCost);

        matrixPanel.setData(currentGrid, currentStart, currentEnd, path);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PathfindingApp::new);
    }
}

class Point3D {
    final int x;
    final int y;
    final int z;

    Point3D(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Point3D other)) return false;
        return x == other.x && y == other.y && z == other.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}

class Grid {
    final int width;
    final int height;
    final int depth;
    final int[][][] weights;

    private final Random random = new Random();

    Grid(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.weights = new int[depth][height][width];
    }

    void randomizeWeights() {
        for (int z = 0; z < depth; z++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    weights[z][y][x] = random.nextInt(9) + 1;
                }
            }
        }
    }

    int getWeight(Point3D p) {
        return weights[p.z][p.y][p.x];
    }

    int calculatePathCost(List<Point3D> path) {
        if (path == null || path.size() < 2) return 0;

        int cost = 0;

        for (int i = 1; i < path.size(); i++) {
            cost += getWeight(path.get(i));
        }

        return cost;
    }

    boolean isInside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0
                && x < width && y < height && z < depth;
    }

    Point3D randomPoint() {
        return new Point3D(
                random.nextInt(width),
                random.nextInt(height),
                random.nextInt(depth)
        );
    }
}

class Pathfinder {

    private static class Node {
        Point3D point;
        int cost;

        Node(Point3D point, int cost) {
            this.point = point;
            this.cost = cost;
        }
    }

    public static List<Point3D> findPath(
            Grid grid,
            Point3D start,
            Point3D end,
            boolean cheapest
    ) {
        int[][][] dist = new int[grid.depth][grid.height][grid.width];
        Point3D[][][] previous = new Point3D[grid.depth][grid.height][grid.width];

        for (int z = 0; z < grid.depth; z++) {
            for (int y = 0; y < grid.height; y++) {
                Arrays.fill(dist[z][y], Integer.MAX_VALUE);
            }
        }

        PriorityQueue<Node> queue = new PriorityQueue<>(
                Comparator.comparingInt(n -> n.cost)
        );

        dist[start.z][start.y][start.x] = 0;
        queue.add(new Node(start, 0));

        int[][] directions = grid.depth == 1
                ? new int[][]{
                {1, 0, 0},
                {-1, 0, 0},
                {0, 1, 0},
                {0, -1, 0}
        }
                : new int[][]{
                {1, 0, 0},
                {-1, 0, 0},
                {0, 1, 0},
                {0, -1, 0},
                {0, 0, 1},
                {0, 0, -1}
        };

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            Point3D p = current.point;

            if (p.equals(end)) break;

            if (current.cost > dist[p.z][p.y][p.x]) continue;

            for (int[] d : directions) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];
                int nz = p.z + d[2];

                if (!grid.isInside(nx, ny, nz)) continue;

                Point3D next = new Point3D(nx, ny, nz);

                int stepCost = cheapest
                        ? grid.getWeight(next)
                        : 1;

                int newCost = dist[p.z][p.y][p.x] + stepCost;

                if (newCost < dist[nz][ny][nx]) {
                    dist[nz][ny][nx] = newCost;
                    previous[nz][ny][nx] = p;
                    queue.add(new Node(next, newCost));
                }
            }
        }

        return restorePath(previous, start, end);
    }

    private static List<Point3D> restorePath(
            Point3D[][][] previous,
            Point3D start,
            Point3D end
    ) {
        List<Point3D> path = new ArrayList<>();

        Point3D current = end;

        while (current != null) {
            path.add(current);

            if (current.equals(start)) break;

            current = previous[current.z][current.y][current.x];
        }

        Collections.reverse(path);

        if (path.isEmpty() || !path.get(0).equals(start)) {
            return Collections.emptyList();
        }

        return path;
    }
}

class MatrixPanel extends JPanel {

    private Grid grid;
    private Point3D start;
    private Point3D end;

    private List<Point3D> pathList = new ArrayList<>();
    private Set<Point3D> pathSet = new HashSet<>();

    private static final int CELL_SIZE_2D = 45;

    private double visualScale = 1.0;

    private double angleX = -0.65;
    private double angleY = 0.75;

    private int lastMouseX;
    private int lastMouseY;

    MatrixPanel() {
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

    void setScale(double visualScale) {
        this.visualScale = visualScale;
        repaint();
    }

    void setData(Grid grid, Point3D start, Point3D end, List<Point3D> path) {
        this.grid = grid;
        this.start = start;
        this.end = end;

        this.pathList = new ArrayList<>(path);
        this.pathSet = new HashSet<>(path);

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (grid == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        if (grid.depth == 1) {
            draw2D(g2);
        } else {
            draw3D(g2);
        }
    }

    private void draw2D(Graphics2D g2) {
        int cellSize = Math.max(8, (int) (CELL_SIZE_2D * visualScale));

        g2.setFont(new Font("Arial", Font.BOLD, Math.max(8, (int) (14 * visualScale))));

        int startX = 40;
        int startY = 40;

        for (int y = 0; y < grid.height; y++) {
            for (int x = 0; x < grid.width; x++) {
                Point3D p = new Point3D(x, y, 0);

                int drawX = startX + x * cellSize;
                int drawY = startY + y * cellSize;

                g2.setColor(getCellColor(p));
                g2.fillRect(drawX, drawY, cellSize, cellSize);

                g2.setColor(Color.BLACK);
                g2.drawRect(drawX, drawY, cellSize, cellSize);

                if (cellSize >= 18) {
                    String text = getCellText(p);

                    FontMetrics fm = g2.getFontMetrics();
                    int textX = drawX + (cellSize - fm.stringWidth(text)) / 2;
                    int textY = drawY + ((cellSize - fm.getHeight()) / 2) + fm.getAscent();

                    g2.drawString(text, textX, textY);
                }
            }
        }
    }

    private void draw3D(Graphics2D g2) {
        drawLegend(g2);

        List<Sphere> spheres = new ArrayList<>();
        List<Cube> pathCubes = new ArrayList<>();

        for (int z = 0; z < grid.depth; z++) {
            for (int y = 0; y < grid.height; y++) {
                for (int x = 0; x < grid.width; x++) {
                    Point3D p = new Point3D(x, y, z);

                    if (pathSet.contains(p)) {
                        pathCubes.add(createCube(p));
                    } else {
                        spheres.add(createSphere(p));
                    }
                }
            }
        }

        spheres.sort(Comparator.comparingDouble(s -> s.projected.depth));
        pathCubes.sort(Comparator.comparingDouble(c -> c.depth));

        for (Sphere sphere : spheres) {
            drawSphere(g2, sphere);
        }

        drawPathLines(g2);

        for (Cube cube : pathCubes) {
            drawCube(g2, cube);
        }
    }

    private void drawLegend(Graphics2D g2) {
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(Color.BLACK);
        g2.drawString("3D mode: drag mouse to rotate", 20, 25);
        g2.drawString("Small gray spheres = unused cells", 20, 45);
        g2.drawString("Green = Start, Red = End, Blue = Path", 20, 65);
    }

    private Sphere createSphere(Point3D p) {
        ProjectedPoint projected = project(p.x, p.y, p.z);
        return new Sphere(p, projected);
    }

    private void drawSphere(Graphics2D g2, Sphere sphere) {
        ProjectedPoint projected = sphere.projected;

        int size = Math.max(3, (int) (12 * projected.scale * visualScale));

        int x = projected.screenX - size / 2;
        int y = projected.screenY - size / 2;

        g2.setColor(new Color(155, 155, 155, 135));
        g2.fillOval(x, y, size, size);

        g2.setColor(new Color(80, 80, 80, 120));
        g2.drawOval(x, y, size, size);

        int highlightSize = Math.max(2, size / 3);
        g2.setColor(new Color(255, 255, 255, 120));
        g2.fillOval(x + size / 5, y + size / 5, highlightSize, highlightSize);
    }

    private Cube createCube(Point3D p) {
        double size = Math.max(0.25, 0.72 * visualScale);

        double x = p.x;
        double y = p.y;
        double z = p.z;

        double[][] corners = {
                {x - size / 2, y - size / 2, z - size / 2},
                {x + size / 2, y - size / 2, z - size / 2},
                {x + size / 2, y + size / 2, z - size / 2},
                {x - size / 2, y + size / 2, z - size / 2},

                {x - size / 2, y - size / 2, z + size / 2},
                {x + size / 2, y - size / 2, z + size / 2},
                {x + size / 2, y + size / 2, z + size / 2},
                {x - size / 2, y + size / 2, z + size / 2}
        };

        ProjectedPoint[] projected = new ProjectedPoint[8];

        double avgDepth = 0;

        for (int i = 0; i < corners.length; i++) {
            projected[i] = project(corners[i][0], corners[i][1], corners[i][2]);
            avgDepth += projected[i].depth;
        }

        avgDepth /= 8.0;

        return new Cube(p, projected, avgDepth);
    }

    private void drawCube(Graphics2D g2, Cube cube) {
        Color base = getCellColor(cube.point);

        drawFace(g2, cube, new int[]{0, 1, 2, 3}, darken(base, 0.78));
        drawFace(g2, cube, new int[]{4, 5, 6, 7}, brighten(base, 1.05));
        drawFace(g2, cube, new int[]{0, 1, 5, 4}, brighten(base, 1.15));
        drawFace(g2, cube, new int[]{2, 3, 7, 6}, darken(base, 0.7));
        drawFace(g2, cube, new int[]{1, 2, 6, 5}, base);
        drawFace(g2, cube, new int[]{0, 3, 7, 4}, darken(base, 0.88));

        drawCubeEdges(g2, cube);

        if (visualScale >= 0.45) {
            draw3DLabel(g2, cube);
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

    private void drawCubeEdges(Graphics2D g2, Cube cube) {
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };

        g2.setStroke(new BasicStroke(Math.max(0.5f, (float) visualScale)));
        g2.setColor(new Color(20, 20, 20, 100));

        for (int[] edge : edges) {
            ProjectedPoint a = cube.points[edge[0]];
            ProjectedPoint b = cube.points[edge[1]];
            g2.drawLine(a.screenX, a.screenY, b.screenX, b.screenY);
        }
    }

    private void draw3DLabel(Graphics2D g2, Cube cube) {
        ProjectedPoint center = project(
                cube.point.x,
                cube.point.y,
                cube.point.z
        );

        String text = getCellText(cube.point);

        g2.setFont(new Font("Arial", Font.BOLD, Math.max(8, (int) (16 * visualScale))));
        FontMetrics fm = g2.getFontMetrics();

        int x = center.screenX - fm.stringWidth(text) / 2;
        int y = center.screenY + fm.getAscent() / 2;

        g2.setColor(Color.BLACK);
        g2.drawString(text, x, y);
    }

    private void drawPathLines(Graphics2D g2) {
        if (pathList.size() < 2) return;

        g2.setStroke(new BasicStroke(
                Math.max(2f, (float) (6f * visualScale)),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
        ));

        g2.setColor(new Color(20, 80, 220, 190));

        for (int i = 1; i < pathList.size(); i++) {
            Point3D aPoint = pathList.get(i - 1);
            Point3D bPoint = pathList.get(i);

            ProjectedPoint a = project(aPoint.x, aPoint.y, aPoint.z);
            ProjectedPoint b = project(bPoint.x, bPoint.y, bPoint.z);

            g2.drawLine(a.screenX, a.screenY, b.screenX, b.screenY);
        }

        g2.setStroke(new BasicStroke(1f));
    }

    private Color getCellColor(Point3D p) {
        if (p.equals(start)) {
            return new Color(70, 190, 90);
        }

        if (p.equals(end)) {
            return new Color(230, 75, 75);
        }

        if (pathSet.contains(p)) {
            return new Color(75, 145, 235);
        }

        int weight = grid.weights[p.z][p.y][p.x];
        int shade = 245 - weight * 14;

        return new Color(shade, shade, shade);
    }

    private String getCellText(Point3D p) {
        if (p.equals(start)) return "S";
        if (p.equals(end)) return "E";

        return String.valueOf(grid.weights[p.z][p.y][p.x]);
    }

    private Color darken(Color color, double factor) {
        return new Color(
                Math.max(0, (int) (color.getRed() * factor)),
                Math.max(0, (int) (color.getGreen() * factor)),
                Math.max(0, (int) (color.getBlue() * factor))
        );
    }

    private Color brighten(Color color, double factor) {
        return new Color(
                Math.min(255, (int) (color.getRed() * factor)),
                Math.min(255, (int) (color.getGreen() * factor)),
                Math.min(255, (int) (color.getBlue() * factor))
        );
    }

    private ProjectedPoint project(double x, double y, double z) {
        double centerX = (grid.width - 1) / 2.0;
        double centerY = (grid.height - 1) / 2.0;
        double centerZ = (grid.depth - 1) / 2.0;

        x -= centerX;
        y -= centerY;
        z -= centerZ;

        double spacing = 62 * visualScale;

        x *= spacing;
        y *= spacing;
        z *= spacing;

        double cosY = Math.cos(angleY);
        double sinY = Math.sin(angleY);

        double rotatedX = x * cosY + z * sinY;
        double rotatedZ = -x * sinY + z * cosY;

        double cosX = Math.cos(angleX);
        double sinX = Math.sin(angleX);

        double rotatedY = y * cosX - rotatedZ * sinX;
        rotatedZ = y * sinX + rotatedZ * cosX;

        double cameraDistance = 850;
        double scale = cameraDistance / (cameraDistance + rotatedZ);

        int screenX = (int) (getWidth() / 2.0 + rotatedX * scale);
        int screenY = (int) (getHeight() / 2.0 + rotatedY * scale);

        return new ProjectedPoint(screenX, screenY, rotatedZ, scale);
    }

    private static class ProjectedPoint {
        int screenX;
        int screenY;
        double depth;
        double scale;

        ProjectedPoint(int screenX, int screenY, double depth, double scale) {
            this.screenX = screenX;
            this.screenY = screenY;
            this.depth = depth;
            this.scale = scale;
        }
    }

    private static class Sphere {
        Point3D point;
        ProjectedPoint projected;

        Sphere(Point3D point, ProjectedPoint projected) {
            this.point = point;
            this.projected = projected;
        }
    }

    private static class Cube {
        Point3D point;
        ProjectedPoint[] points;
        double depth;

        Cube(Point3D point, ProjectedPoint[] points, double depth) {
            this.point = point;
            this.points = points;
            this.depth = depth;
        }
    }
}