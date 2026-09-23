package v2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RenderUtilsTest {

    @Test
    void getCellColorHighlightsStartEndPathAndWeights() {
        Grid grid = new Grid(2, 2, 1);
        grid.weights[0][0][0] = 1;
        grid.weights[0][0][1] = 9;
        grid.weights[0][1][0] = 5;

        Point3D start = new Point3D(0, 0, 0);
        Point3D end = new Point3D(1, 0, 0);
        Point3D mid = new Point3D(0, 1, 0);
        List<Point3D> path = List.of(start, mid, end);

        RenderData data = new RenderData(
                grid,
                PathType.SHORTEST,
                start,
                end,
                path,
                Set.of(),
                null,
                false,
                false
        );

        assertEquals(new Color(70, 190, 90), RenderUtils.getCellColor(data, start, data.pathSet));
        assertEquals(new Color(230, 75, 75), RenderUtils.getCellColor(data, end, data.pathSet));
        assertEquals(new Color(75, 145, 235), RenderUtils.getCellColor(data, mid, data.pathSet));
    }

    @Test
    void getCellColorUsesGridWeightForNonPathCells() {
        Grid grid = new Grid(1, 1, 1);
        grid.weights[0][0][0] = 7;

        RenderData data = new RenderData(
                grid,
                PathType.SHORTEST,
                null,
                null,
                List.of(),
                Set.of(),
                null,
                false,
                false
        );

        assertEquals(new Color(147, 147, 147), RenderUtils.getCellColor(data, new Point3D(0, 0, 0), data.pathSet));
    }

    @Test
    void interactiveCellColorsPreferSpecialStates() {
        Grid grid = new Grid(3, 2, 1);
        grid.weights[0][1][0] = 1;
        grid.weights[0][1][1] = 1;
        Point3D start = new Point3D(0, 0, 0);
        Point3D end = new Point3D(2, 1, 0);
        Point3D selected = new Point3D(1, 0, 0);
        Point3D frontier = new Point3D(0, 1, 0);
        Point3D pathPoint = new Point3D(1, 1, 0);

        RenderData data = new RenderData(
                grid,
                PathType.INTERACTIVE,
                start,
                end,
                List.of(start, pathPoint, end),
                Set.of(frontier),
                selected,
                false,
                false
        );

        assertEquals(new Color(70, 190, 90), RenderUtils.getCellColor(data, start, data.pathSet));
        assertEquals(new Color(230, 75, 75), RenderUtils.getCellColor(data, end, data.pathSet));
        assertEquals(new Color(70, 190, 90), RenderUtils.getCellColor(data, selected, data.pathSet));
        assertEquals(new Color(75, 145, 235), RenderUtils.getCellColor(data, pathPoint, data.pathSet));
        assertEquals(new Color(70, 190, 90), RenderUtils.getCellColor(data, frontier, data.pathSet));
    }

    @Test
    void getCellTextMatchesInteractiveStates() {
        Grid grid = new Grid(3, 2, 1);
        grid.weights[0][1][0] = 1;
        grid.weights[0][1][1] = 1;
        Point3D start = new Point3D(0, 0, 0);
        Point3D end = new Point3D(2, 1, 0);
        Point3D selected = new Point3D(1, 0, 0);
        Point3D frontier = new Point3D(0, 1, 0);
        Point3D pathPoint = new Point3D(1, 1, 0);

        RenderData data = new RenderData(
                grid,
                PathType.INTERACTIVE,
                start,
                end,
                List.of(start, pathPoint, end),
                Set.of(frontier),
                selected,
                false,
                false
        );

        assertEquals("S", RenderUtils.getCellText(data, start));
        assertEquals("E", RenderUtils.getCellText(data, end));
        assertEquals(">", RenderUtils.getCellText(data, selected));
        assertEquals("", RenderUtils.getCellText(data, pathPoint));
        assertEquals("1", RenderUtils.getCellText(data, frontier));
    }

    @Test
    void surfaceAndInteractiveClassificationWorks() {
        Grid grid = new Grid(3, 3, 3);
        RenderData data = new RenderData(
                grid,
                PathType.INTERACTIVE,
                new Point3D(0, 0, 0),
                new Point3D(2, 2, 2),
                List.of(new Point3D(0, 0, 0)),
                Set.of(),
                null,
                false,
                false
        );

        assertTrue(RenderUtils.isSurfaceNode(grid, new Point3D(0, 1, 1)));
        assertFalse(RenderUtils.isSurfaceNode(grid, new Point3D(1, 1, 1)));
        assertTrue(RenderUtils.isInteractiveBox(data, new Point3D(0, 1, 1)));
        assertTrue(RenderUtils.isInteractiveSphere(data, new Point3D(1, 1, 1)));
        assertTrue(RenderUtils.isInteractiveVisible(data, new Point3D(1, 1, 1)));
    }

    @Test
    void darkenAndBrightenClampChannels() {
        Color color = new Color(100, 150, 200);

        assertEquals(new Color(50, 75, 100), RenderUtils.darken(color, 0.5));
        assertEquals(new Color(110, 165, 220), RenderUtils.brighten(color, 1.1));
        assertEquals(new Color(0, 0, 0), RenderUtils.darken(color, 0.0));
        assertEquals(new Color(255, 255, 255), RenderUtils.brighten(new Color(250, 250, 250), 2.0));
    }

    @ParameterizedTest
    @EnumSource(PathType.class)
    void pausedStepsAddAndRemovePathHighlight(PathType pathType) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Grid grid = new Grid(3, 1, 1);
            grid.weights[0][0][1] = 7;
            Point3D start = new Point3D(0, 0, 0);
            Point3D middle = new Point3D(1, 0, 0);
            Point3D end = new Point3D(2, 0, 0);
            List<Point3D> path = List.of(start, middle, end);
            DemoPlayback playback = new DemoPlayback(() -> 60_000, () -> 60_000, direction -> {}, () -> {});
            try {
                playback.start(path.size());
                playback.togglePaused();
                RenderData initial = demoData(grid, pathType, path, playback, false);
                Color unusedColor = RenderUtils.getCellColor(initial, middle, initial.pathSet);

                playback.next();
                RenderData advanced = demoData(grid, pathType, path, playback, false);
                assertEquals(new Color(75, 145, 235), RenderUtils.getCellColor(advanced, middle, advanced.pathSet));
                assertTrue(RenderUtils.pathContains(advanced, middle));

                playback.previous();
                RenderData rewound = demoData(grid, pathType, path, playback, false);
                assertEquals(unusedColor, RenderUtils.getCellColor(rewound, middle, rewound.pathSet));
                assertFalse(RenderUtils.pathContains(rewound, middle));
                assertEquals(new Color(70, 190, 90), RenderUtils.getCellColor(rewound, start, rewound.pathSet));
                assertEquals(new Color(230, 75, 75), RenderUtils.getCellColor(rewound, end, rewound.pathSet));
                assertEquals("S", RenderUtils.getCellText(rewound, start));
                assertEquals("E", RenderUtils.getCellText(rewound, end));
                if (pathType == PathType.INTERACTIVE) {
                    assertFalse(RenderUtils.isInteractiveVisible(rewound, middle));
                }
            } finally {
                playback.stop();
            }
        });
    }

    @ParameterizedTest
    @EnumSource(PathType.class)
    void pausedPreviewFollowsNextNodeWhenSteppingForwardAndBackward(PathType pathType) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Grid grid = new Grid(3, 1, 2);
            Point3D start = new Point3D(0, 0, 0);
            Point3D middle = new Point3D(1, 0, 0);
            Point3D end = new Point3D(2, 0, 0);
            List<Point3D> path = List.of(start, middle, end);
            Color previewColor = new Color(45, 105, 245);
            DemoPlayback playback = new DemoPlayback(() -> 60_000, () -> 60_000, direction -> {}, () -> {});
            try {
                playback.start(path.size());
                playback.togglePaused();
                RenderData initial = demoData(grid, pathType, path, playback, true);
                assertEquals(previewColor, RenderUtils.getCellColor(initial, middle, initial.pathSet));

                playback.next();
                RenderData advanced = demoData(grid, pathType, path, playback, true);
                assertEquals(new Color(75, 145, 235), RenderUtils.getCellColor(advanced, middle, advanced.pathSet));
                assertEquals(previewColor, RenderUtils.getCellColor(advanced, end, advanced.pathSet));

                playback.next();
                RenderData complete = demoData(grid, pathType, path, playback, true);
                assertEquals(new Color(230, 75, 75), RenderUtils.getCellColor(complete, end, complete.pathSet));

                playback.previous();
                playback.previous();
                RenderData rewound = demoData(grid, pathType, path, playback, true);
                assertEquals(previewColor, RenderUtils.getCellColor(rewound, middle, rewound.pathSet));
                assertEquals(new Color(230, 75, 75), RenderUtils.getCellColor(rewound, end, rewound.pathSet));
                if (pathType == PathType.INTERACTIVE) {
                    assertTrue(RenderUtils.isInteractiveVisible(rewound, middle));
                }
            } finally {
                playback.stop();
            }
        });
    }

    private RenderData demoData(Grid grid, PathType pathType, List<Point3D> path,
                                DemoPlayback playback, boolean showPreview) {
        int visibleNodes = playback.visibleNodes();
        Point3D preview = showPreview && visibleNodes < path.size() ? path.get(visibleNodes) : null;
        return new RenderData(
                grid, pathType, path.getFirst(), path.getLast(), List.copyOf(path.subList(0, visibleNodes)),
                Set.of(), null, visibleNodes == path.size(), true, preview, preview != null
        );
    }
}
