package v2;

import org.junit.jupiter.api.Test;

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
}
