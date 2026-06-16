package v2;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {

    private final JCheckBox mode3DCheckBox = new JCheckBox("3D mode");
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

    public MainFrame() {
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

        depthField.setEnabled(false);
        rerollPointsButton.setEnabled(false);

        add(controls, BorderLayout.NORTH);
        add(matrixPanel, BorderLayout.CENTER);

        generateButton.addActionListener(e -> generateMatrix());
        rerollPointsButton.addActionListener(e -> rerollStartEnd());

        mode3DCheckBox.addActionListener(e ->
                depthField.setEnabled(mode3DCheckBox.isSelected())
        );

        pathTypeBox.addActionListener(e -> {
            if (currentGrid != null) rebuildPath();
        });

        scaleSlider.addChangeListener(e ->
                matrixPanel.setScale(scaleSlider.getValue() / 100.0)
        );

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
        currentStart = currentGrid.randomPoint();

        do {
            currentEnd = currentGrid.randomPoint();
        } while (currentEnd.equals(currentStart));

        rebuildPath();
    }

    private void rebuildPath() {
        PathType pathType = (PathType) pathTypeBox.getSelectedItem();

        List<Point3D> path = Pathfinder.findPath(
                currentGrid,
                currentStart,
                currentEnd,
                pathType
        );

        pathLengthLabel.setText("Length: " + Math.max(0, path.size() - 1));
        pathCostLabel.setText("Cost: " + currentGrid.calculatePathCost(path));

        matrixPanel.setData(new RenderData(currentGrid, currentStart, currentEnd, path));
    }
}
