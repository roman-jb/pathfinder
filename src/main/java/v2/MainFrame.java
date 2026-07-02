package v2;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainFrame extends JFrame {

    private final JCheckBox mode3DCheckBox = new JCheckBox("3D mode");
    private final JCheckBox hideUnusedNodesCheckBox = new JCheckBox("Hide unused nodes");
    private final JComboBox<PathType> pathTypeBox = new JComboBox<>(PathType.values());

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
    private List<Point3D> interactivePath = new ArrayList<>();
    private Point3D interactivePending;
    private boolean interactiveComplete;

    public MainFrame() {
        super("Pathfinding 2D / 3D");

        JButton generateButton = new JButton("Generate Matrix");

        JPanel controls = new JPanel();

        controls.add(mode3DCheckBox);
        controls.add(hideUnusedNodesCheckBox);

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

        depthField.setEnabled(false);
        rerollPointsButton.setEnabled(false);

        add(controls, BorderLayout.NORTH);
        add(matrixPanel, BorderLayout.CENTER);

        matrixPanel.setClickHandler(this::handleNodeClick);
        matrixPanel.setWheelHandler(notches -> {
            int step = Math.max(1, scaleSlider.getMinorTickSpacing());
            int delta = -notches * step;
            int value = Math.max(scaleSlider.getMinimum(), Math.min(scaleSlider.getMaximum(), scaleSlider.getValue() + delta));
            scaleSlider.setValue(value);
        });

        generateButton.addActionListener(e -> generateMatrix());
        rerollPointsButton.addActionListener(e -> rerollStartEnd());

        mode3DCheckBox.addActionListener(e ->
                depthField.setEnabled(mode3DCheckBox.isSelected())
        );

        pathTypeBox.addActionListener(e -> {
            if (currentGrid != null) {
                syncPathStateToSelection();
                refreshDisplay();
            }
        });

        hideUnusedNodesCheckBox.addActionListener(e -> {
            if (currentGrid != null) {
                refreshDisplay();
            }
        });

        scaleSlider.addChangeListener(e ->
                matrixPanel.setScale(scaleSlider.getValue() / 100.0)
        );

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1600, 900);
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

        int cellCount = currentGrid.width * currentGrid.height * currentGrid.depth;
        if (cellCount <= 1) {
            currentEnd = currentStart;
        } else {
            do {
                currentEnd = currentGrid.randomPoint();
            } while (currentEnd.equals(currentStart));
        }

        syncPathStateToSelection();
        refreshDisplay();
    }

    private void refreshDisplay() {
        if (currentGrid == null || currentStart == null || currentEnd == null) {
            return;
        }

        PathType pathType = currentPathType();

        if (pathType == PathType.INTERACTIVE) {
            Set<Point3D> frontier = getInteractiveFrontier();

            pathLengthLabel.setText("Length: " + Math.max(0, interactivePath.size() - 1));
            pathCostLabel.setText("Cost: " + currentGrid.calculatePathCost(interactivePath));

            matrixPanel.setData(new RenderData(
                    currentGrid,
                    pathType,
                    currentStart,
                    currentEnd,
                    new ArrayList<>(interactivePath),
                    frontier,
                    interactivePending,
                    interactiveComplete,
                    hideUnusedNodesCheckBox.isSelected()
            ));
            return;
        }

        List<Point3D> path = Pathfinder.findPath(
                currentGrid,
                currentStart,
                currentEnd,
                pathType
        );

        pathLengthLabel.setText("Length: " + Math.max(0, path.size() - 1));
        pathCostLabel.setText("Cost: " + currentGrid.calculatePathCost(path));

        matrixPanel.setData(new RenderData(
                currentGrid,
                pathType,
                currentStart,
                currentEnd,
                path,
                Set.of(),
                null,
                false,
                hideUnusedNodesCheckBox.isSelected()
        ));
    }

    private void syncPathStateToSelection() {
        if (currentPathType() == PathType.INTERACTIVE) {
            initializeInteractiveState();
        } else {
            interactivePath = new ArrayList<>();
            interactivePending = null;
            interactiveComplete = false;
        }
    }

    private void initializeInteractiveState() {
        interactivePath = new ArrayList<>();
        if (currentStart != null) {
            interactivePath.add(currentStart);
        }
        interactivePending = null;
        interactiveComplete = currentStart != null && currentStart.equals(currentEnd);
    }

    private PathType currentPathType() {
        return (PathType) pathTypeBox.getSelectedItem();
    }

    private void handleNodeClick(Point3D point) {
        if (currentGrid == null
                || currentPathType() != PathType.INTERACTIVE
                || interactiveComplete) {
            return;
        }

        Set<Point3D> frontier = getInteractiveFrontier();
        if (!frontier.contains(point)) {
            return;
        }

        if (point.equals(interactivePending)) {
            interactivePath.add(point);
            interactivePending = null;
            interactiveComplete = point.equals(currentEnd);
        } else {
            interactivePending = point;
        }

        refreshDisplay();
    }

    private Set<Point3D> getInteractiveFrontier() {
        if (currentGrid == null || interactivePath.isEmpty() || interactiveComplete) {
            return Set.of();
        }

        Point3D anchor = interactivePath.getLast();
        Set<Point3D> frontier = new HashSet<>();

        for (Point3D neighbor : getNeighbors(anchor)) {
            if (!interactivePath.contains(neighbor)) {
                frontier.add(neighbor);
            }
        }

        return frontier;
    }

    private List<Point3D> getNeighbors(Point3D point) {
        List<Point3D> neighbors = new ArrayList<>();

        int[][] directions = currentGrid.depth == 1
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

        for (int[] direction : directions) {
            int nx = point.x + direction[0];
            int ny = point.y + direction[1];
            int nz = point.z + direction[2];

            if (currentGrid.isInside(nx, ny, nz)) {
                neighbors.add(new Point3D(nx, ny, nz));
            }
        }

        return neighbors;
    }
}
