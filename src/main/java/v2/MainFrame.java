package v2;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainFrame extends JFrame {

    private static final List<Image> APPLICATION_ICONS = loadApplicationIcons();

    private final JCheckBox mode3DCheckBox = new JCheckBox("3D mode");
    private final JCheckBox hideUnusedNodesCheckBox = new JCheckBox("Hide unused nodes");
    private final JCheckBox demoModeCheckBox = new JCheckBox("DEMO MODE");
    private final JCheckBox thirdPersonDemoCheckBox = new JCheckBox("DEMO MODE - 3rd person camera");
    private final JComboBox<PathType> pathTypeBox = new JComboBox<>(PathType.values());

    private final JTextField widthField = new JTextField("8", 4);
    private final JTextField heightField = new JTextField("8", 4);
    private final JTextField depthField = new JTextField("4", 4);
    private final JTextField stepDelayField = new JTextField("250", 5);
    private final JTextField cycleDelayField = new JTextField("1000", 5);

    private final JSlider scaleSlider = new JSlider(30, 150, 100);

    private final JButton generateButton = new JButton("Generate Matrix");
    private final JButton rerollPointsButton = new JButton("New Start / End");

    private final JLabel pathLengthLabel = new JLabel("Length: -");
    private final JLabel pathCostLabel = new JLabel("Cost: -");

    private final MatrixPanel matrixPanel = new MatrixPanel();
    private final JPanel mainControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final Map<UiElement, JPanel> uiElementPanels = new EnumMap<>(UiElement.class);
    private final Map<UiElement, Boolean> uiElementVisibility = new EnumMap<>(UiElement.class);

    private JDialog optionsDialog;
    private JPanel optionsControlsPanel;

    private Grid currentGrid;
    private Point3D currentStart;
    private Point3D currentEnd;
    private List<Point3D> interactivePath = new ArrayList<>();
    private Point3D interactivePending;
    private boolean interactiveComplete;

    private Timer demoTimer;
    private Timer demoBlinkTimer;
    private List<Point3D> demoPath = List.of();
    private int demoVisibleNodes;
    private boolean demoPreviewVisible;

    public MainFrame() {
        super("Pathfinding 2D / 3D");
        applyApplicationIcons(this);
        applyTaskbarIcon();

        registerUiElements();

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(mainControlsPanel, BorderLayout.CENTER);

        JPanel optionsButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton optionsButton = new JButton("OPTIONS");
        optionsButtonPanel.add(optionsButton);
        topBar.add(optionsButtonPanel, BorderLayout.EAST);

        rebuildControlLocations();

        depthField.setEnabled(false);
        rerollPointsButton.setEnabled(false);

        add(topBar, BorderLayout.NORTH);
        add(matrixPanel, BorderLayout.CENTER);

        matrixPanel.setClickHandler(this::handleNodeClick);
        matrixPanel.setWheelHandler(notches -> {
            int step = Math.max(1, scaleSlider.getMinorTickSpacing());
            int delta = -notches * step;
            int value = Math.clamp(scaleSlider.getValue() + delta, scaleSlider.getMinimum(), scaleSlider.getMaximum());
            scaleSlider.setValue(value);
        });

        generateButton.addActionListener(e -> generateMatrix());
        rerollPointsButton.addActionListener(e -> rerollStartEnd());
        optionsButton.addActionListener(e -> showOptionsWindow());

        demoModeCheckBox.addActionListener(e -> {
            if (demoModeCheckBox.isSelected()) {
                if (currentGrid != null && currentStart != null && currentEnd != null) {
                    beginPathAnimation();
                }
            } else {
                stopDemo();
                syncPathStateToSelection();
                refreshDisplay();
            }
        });

        thirdPersonDemoCheckBox.addActionListener(e -> {
            if (demoModeCheckBox.isSelected()
                    && currentGrid != null
                    && currentStart != null
                    && currentEnd != null) {
                int stepDelay = readDelay(stepDelayField, 250);
                prepareDemoCamera(false, stepDelay);
                renderDemoProgress();
            }
        });

        mode3DCheckBox.addActionListener(e ->
                depthField.setEnabled(mode3DCheckBox.isSelected())
        );

        pathTypeBox.addActionListener(e -> {
            if (currentGrid != null) {
                if (demoModeCheckBox.isSelected()) {
                    if (currentStart != null && currentEnd != null) {
                        beginPathAnimation();
                    } else {
                        renderMatrixWithoutPoints();
                    }
                } else {
                    syncPathStateToSelection();
                    refreshDisplay();
                }
            }
        });

        hideUnusedNodesCheckBox.addActionListener(e -> {
            if (currentGrid != null) {
                if (currentStart == null || currentEnd == null) {
                    renderMatrixWithoutPoints();
                } else if (demoModeCheckBox.isSelected()) {
                    renderDemoProgress();
                } else {
                    refreshDisplay();
                }
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

    private void registerUiElements() {
        registerUiElement(UiElement.MODE_3D, controlPanel(mode3DCheckBox));
        registerUiElement(UiElement.HIDE_UNUSED_NODES, controlPanel(hideUnusedNodesCheckBox));
        registerUiElement(UiElement.DEMO_MODE, controlPanel(demoModeCheckBox));
        registerUiElement(
                UiElement.THIRD_PERSON_DEMO_CAMERA,
                controlPanel(thirdPersonDemoCheckBox),
                false
        );
        registerUiElement(UiElement.PATH_TYPE, labeledControl("Path type:", pathTypeBox));
        registerUiElement(UiElement.WIDTH, labeledControl("Width X:", widthField));
        registerUiElement(UiElement.HEIGHT, labeledControl("Height Y:", heightField));
        registerUiElement(UiElement.DEPTH, labeledControl("Depth Z:", depthField));
        registerUiElement(UiElement.SCALE, labeledControl("Scale:", scaleSlider));
        registerUiElement(
                UiElement.STEP_DELAY,
                labeledControl("Step delay (ms):", stepDelayField),
                false
        );
        registerUiElement(
                UiElement.CYCLE_DELAY,
                labeledControl("Cycle delay (ms):", cycleDelayField),
                false
        );
        registerUiElement(UiElement.GENERATE_MATRIX, controlPanel(generateButton));
        registerUiElement(UiElement.NEW_START_END, controlPanel(rerollPointsButton));
        registerUiElement(UiElement.PATH_LENGTH, controlPanel(pathLengthLabel));
        registerUiElement(UiElement.PATH_COST, controlPanel(pathCostLabel));
    }

    private void registerUiElement(UiElement element, JPanel panel) {
        registerUiElement(element, panel, true);
    }

    private void registerUiElement(UiElement element, JPanel panel, boolean visibleInMainWindow) {
        uiElementPanels.put(element, panel);
        uiElementVisibility.put(element, visibleInMainWindow);
    }

    private JPanel labeledControl(String label, JComponent component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        panel.add(new JLabel(label));
        panel.add(component);
        return panel;
    }

    private JPanel controlPanel(JComponent component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.add(component);
        return panel;
    }

    private void showOptionsWindow() {
        if (optionsDialog == null) {
            createOptionsWindow();
        }

        rebuildControlLocations();
        optionsDialog.setLocationRelativeTo(this);
        optionsDialog.setVisible(true);
        optionsDialog.toFront();
    }

    private void createOptionsWindow() {
        optionsDialog = new JDialog(this, "OPTIONS", false);
        applyApplicationIcons(optionsDialog);
        optionsDialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        optionsDialog.setLayout(new BorderLayout(8, 8));

        optionsControlsPanel = new JPanel();
        optionsControlsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        optionsControlsPanel.setLayout(new BoxLayout(optionsControlsPanel, BoxLayout.Y_AXIS));
        optionsDialog.add(new JScrollPane(optionsControlsPanel), BorderLayout.CENTER);

        JButton editUiButton = new JButton("Edit UI");
        editUiButton.addActionListener(e -> showEditUiWindow());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        bottomPanel.add(editUiButton, BorderLayout.WEST);
        optionsDialog.add(bottomPanel, BorderLayout.SOUTH);

        optionsDialog.setSize(900, 300);
    }

    private void showEditUiWindow() {
        JDialog editDialog = new JDialog(optionsDialog, "Edit UI", Dialog.ModalityType.APPLICATION_MODAL);
        applyApplicationIcons(editDialog);
        editDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        editDialog.setLayout(new BorderLayout(8, 8));

        JPanel listPanel = new JPanel();
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        Map<UiElement, JCheckBox> checkBoxes = new LinkedHashMap<>();
        for (UiElement element : UiElement.values()) {
            JCheckBox checkBox = new JCheckBox(element.displayName, uiElementVisibility.get(element));
            checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkBoxes.put(element, checkBox);
            listPanel.add(checkBox);
        }

        editDialog.add(new JScrollPane(listPanel), BorderLayout.CENTER);

        JPanel bottomPanel = getBottomPanel(editDialog, checkBoxes);
        editDialog.add(bottomPanel, BorderLayout.SOUTH);

        editDialog.setSize(400, 520);
        editDialog.setLocationRelativeTo(optionsDialog);
        editDialog.setVisible(true);
    }

    private static List<Image> loadApplicationIcons() {
        try (InputStream input = MainFrame.class.getResourceAsStream("/v2/app-icon.png")) {
            if (input == null) return List.of();

            BufferedImage source = ImageIO.read(input);
            if (source == null) return List.of();

            int[] sizes = {16, 20, 24, 32, 40, 48, 64, 128, 256};
            List<Image> icons = new ArrayList<>(sizes.length + 1);
            for (int size : sizes) {
                BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = scaled.createGraphics();
                graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC
                );
                graphics.drawImage(source, 0, 0, size, size, null);
                graphics.dispose();
                icons.add(scaled);
            }
            icons.add(source);
            return List.copyOf(icons);
        } catch (IOException exception) {
            return List.of();
        }
    }

    private static void applyApplicationIcons(Window window) {
        if (!APPLICATION_ICONS.isEmpty()) {
            window.setIconImages(APPLICATION_ICONS);
        }
    }

    private static void applyTaskbarIcon() {
        if (APPLICATION_ICONS.isEmpty() || !Taskbar.isTaskbarSupported()) return;

        Taskbar taskbar = Taskbar.getTaskbar();
        if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            try {
                taskbar.setIconImage(APPLICATION_ICONS.getLast());
            } catch (SecurityException | UnsupportedOperationException ignored) {
                // Window icons still apply when the platform disallows changing the taskbar icon.
            }
        }
    }

    private JPanel getBottomPanel(JDialog editDialog, Map<UiElement, JCheckBox> checkBoxes) {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> editDialog.dispose());

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            for (Map.Entry<UiElement, JCheckBox> entry : checkBoxes.entrySet()) {
                uiElementVisibility.put(entry.getKey(), entry.getValue().isSelected());
            }
            rebuildControlLocations();
            editDialog.dispose();
        });

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 12, 10, 12));
        bottomPanel.add(cancelButton, BorderLayout.WEST);
        bottomPanel.add(okButton, BorderLayout.EAST);
        return bottomPanel;
    }

    private void rebuildControlLocations() {
        mainControlsPanel.removeAll();
        if (optionsControlsPanel != null) {
            optionsControlsPanel.removeAll();
        }

        int hiddenCount = 0;
        for (UiElement element : UiElement.values()) {
            JPanel panel = uiElementPanels.get(element);
            if (uiElementVisibility.get(element)) {
                mainControlsPanel.add(panel);
            } else {
                hiddenCount++;
                if (optionsControlsPanel != null) {
                    if (optionsControlsPanel.getComponentCount() > 0) {
                        int lineHeight = optionsControlsPanel
                                .getFontMetrics(optionsControlsPanel.getFont())
                                .getHeight();
                        int rowSpacing = Math.max(1, (int) Math.round(lineHeight * 0.30));
                        optionsControlsPanel.add(Box.createRigidArea(new Dimension(0, rowSpacing)));
                    }
                    panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    Dimension preferredSize = panel.getPreferredSize();
                    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredSize.height));
                    optionsControlsPanel.add(panel);
                }
            }
        }

        if (optionsControlsPanel != null && hiddenCount == 0) {
            JLabel emptyLabel = new JLabel("All configurable UI elements are displayed in the main window.");
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            optionsControlsPanel.add(emptyLabel);
        }

        mainControlsPanel.revalidate();
        mainControlsPanel.repaint();
        if (optionsControlsPanel != null) {
            optionsControlsPanel.revalidate();
            optionsControlsPanel.repaint();
        }
    }

    private enum UiElement {
        MODE_3D("3D mode"),
        HIDE_UNUSED_NODES("Hide unused nodes"),
        DEMO_MODE("DEMO MODE"),
        THIRD_PERSON_DEMO_CAMERA("DEMO MODE - 3rd person camera"),
        PATH_TYPE("Path type"),
        WIDTH("Width X"),
        HEIGHT("Height Y"),
        DEPTH("Depth Z"),
        SCALE("Scale"),
        STEP_DELAY("Step delay"),
        CYCLE_DELAY("Cycle delay"),
        GENERATE_MATRIX("Generate Matrix"),
        NEW_START_END("New Start / End"),
        PATH_LENGTH("Path length"),
        PATH_COST("Path cost");

        private final String displayName;

        UiElement(String displayName) {
            this.displayName = displayName;
        }
    }

    private boolean generateMatrix() {
        boolean is3D = mode3DCheckBox.isSelected();

        int width;
        int height;
        int depth;
        try {
            width = parsePositiveInt(widthField, "Width");
            height = parsePositiveInt(heightField, "Height");
            depth = is3D ? parsePositiveInt(depthField, "Depth") : 1;
        } catch (IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(), "Invalid matrix size", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        currentGrid = new Grid(width, height, depth);
        currentGrid.randomizeWeights();

        stopDemo();
        currentStart = null;
        currentEnd = null;
        interactivePath = new ArrayList<>();
        interactivePending = null;
        interactiveComplete = false;

        rerollPointsButton.setEnabled(true);
        pathLengthLabel.setText("Length: -");
        pathCostLabel.setText("Cost: -");
        renderMatrixWithoutPoints();
        return true;
    }

    private void renderMatrixWithoutPoints() {
        matrixPanel.setData(new RenderData(
                currentGrid,
                currentPathType(),
                null,
                null,
                List.of(),
                Set.of(),
                null,
                false,
                hideUnusedNodesCheckBox.isSelected()
        ));
    }

    private void rerollStartEnd() {
        if (currentGrid == null) return;

        chooseRandomStartEnd();

        if (demoModeCheckBox.isSelected()) {
            beginPathAnimation();
        } else {
            syncPathStateToSelection();
            refreshDisplay();
        }
    }

    private void chooseRandomStartEnd() {
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

    }

    private void startDemoCycle() {
        if (!demoModeCheckBox.isSelected() || currentGrid == null) return;

        stopDemoTimer();
        chooseRandomStartEnd();
        beginPathAnimation();
    }

    private void beginPathAnimation() {
        stopDemoTimer();

        demoPath = Pathfinder.findPath(currentGrid, currentStart, currentEnd, currentPathType());
        demoVisibleNodes = Math.min(1, demoPath.size());
        int stepDelay = readDelay(stepDelayField, 250);
        prepareDemoCamera(false, stepDelay);
        renderDemoProgress();
        if (demoVisibleNodes < demoPath.size()) {
            scheduleNextDemoStep(stepDelay);
        } else {
            scheduleNextDemoCycle(readDelay(cycleDelayField, 1000));
        }
    }

    private void scheduleNextDemoStep(int delay) {
        if (!demoModeCheckBox.isSelected()) return;

        demoTimer = new Timer(delay, e -> advanceDemo());
        demoTimer.setRepeats(false);
        demoTimer.start();
    }

    private void advanceDemo() {
        if (!demoModeCheckBox.isSelected()) return;

        if (demoVisibleNodes < demoPath.size()) {
            Point3D previous = demoPath.get(demoVisibleNodes - 1);
            Point3D next = demoPath.get(demoVisibleNodes);
            stopDemoBlinking();
            demoVisibleNodes++;

            int stepDelay = readDelay(stepDelayField, 250);
            if (isThirdPersonDemoActive()) {
                prepareDemoCamera(true, stepDelay);
            } else if (mode3DCheckBox.isSelected() && currentGrid.depth > 1) {
                matrixPanel.followPath(previous, next);
            }

            renderDemoProgress();
            if (demoVisibleNodes < demoPath.size()) {
                scheduleNextDemoStep(stepDelay);
            } else {
                scheduleNextDemoCycle(readDelay(cycleDelayField, 1000));
            }
        }
    }

    private void scheduleNextDemoCycle(int delay) {
        demoTimer = new Timer(delay, e -> startDemoCycle());
        demoTimer.setRepeats(false);
        demoTimer.start();
    }

    private void renderDemoProgress() {
        List<Point3D> visiblePath = new ArrayList<>(demoPath.subList(0, demoVisibleNodes));
        Point3D previewPoint = isThirdPersonDemoActive() && demoVisibleNodes < demoPath.size()
                ? demoPath.get(demoVisibleNodes)
                : null;

        pathLengthLabel.setText("Length: " + Math.max(0, visiblePath.size() - 1));
        pathCostLabel.setText("Cost: " + currentGrid.calculatePathCost(visiblePath));

        matrixPanel.setData(new RenderData(
                currentGrid,
                currentPathType(),
                currentStart,
                currentEnd,
                visiblePath,
                Set.of(),
                null,
                demoVisibleNodes == demoPath.size(),
                hideUnusedNodesCheckBox.isSelected(),
                previewPoint,
                previewPoint != null && demoPreviewVisible
        ));
    }

    private void prepareDemoCamera(boolean animateMovement, int stepDelay) {
        stopDemoBlinking();

        if (!isThirdPersonDemoActive() || demoVisibleNodes == 0) {
            demoPreviewVisible = false;
            matrixPanel.clearThirdPersonCamera();
            return;
        }

        Point3D current = demoPath.get(demoVisibleNodes - 1);
        Point3D next = demoVisibleNodes < demoPath.size() ? demoPath.get(demoVisibleNodes) : null;
        int movementDuration = animateMovement && stepDelay > 0
                ? Math.clamp((long) stepDelay * 2 / 3, 80, 400)
                : 0;
        matrixPanel.setThirdPersonCamera(current, next, animateMovement, movementDuration);

        if (next != null) {
            startDemoBlinking(stepDelay);
        }
    }

    private boolean isThirdPersonDemoActive() {
        return thirdPersonDemoCheckBox.isSelected()
                && demoModeCheckBox.isSelected()
                && mode3DCheckBox.isSelected()
                && currentGrid != null
                && currentGrid.depth > 1;
    }

    private void startDemoBlinking(int stepDelay) {
        demoPreviewVisible = true;
        if (stepDelay <= 0) return;

        int blinkInterval = Math.clamp(Math.max(1, stepDelay / 3), 40, 250);
        demoBlinkTimer = new Timer(blinkInterval, e -> {
            demoPreviewVisible = !demoPreviewVisible;
            renderDemoProgress();
        });
        demoBlinkTimer.start();
    }

    private void stopDemoBlinking() {
        if (demoBlinkTimer != null) {
            demoBlinkTimer.stop();
            demoBlinkTimer = null;
        }
        demoPreviewVisible = false;
    }

    private void stopDemo() {
        stopDemoTimer();
        stopDemoBlinking();
        demoPath = List.of();
        demoVisibleNodes = 0;
        matrixPanel.clearThirdPersonCamera();
    }

    private void stopDemoTimer() {
        if (demoTimer != null) {
            demoTimer.stop();
            demoTimer = null;
        }
    }

    private int parsePositiveInt(JTextField field, String name) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value <= 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a positive whole number.");
        }
    }

    private int readDelay(JTextField field, int fallback) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            field.setText(Integer.toString(fallback));
            return fallback;
        }
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
                || demoModeCheckBox.isSelected()
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
            int nx = point.x() + direction[0];
            int ny = point.y() + direction[1];
            int nz = point.z() + direction[2];

            if (currentGrid.isInside(nx, ny, nz)) {
                neighbors.add(new Point3D(nx, ny, nz));
            }
        }

        return neighbors;
    }
}
