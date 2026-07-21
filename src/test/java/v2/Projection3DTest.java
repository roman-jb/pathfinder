package v2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Projection3DTest {

    @Test
    void projectsGridCenterToPanelCenter() {
        Grid grid = new Grid(1, 1, 1);
        Projection3D projection = new Projection3D(grid, 1.0, 0.0, 0.0, 800, 600);

        ProjectedPoint projected = projection.project(0, 0, 0);

        assertEquals(400, projected.screenX);
        assertEquals(300, projected.screenY);
        assertEquals(0.0, projected.depth, 1e-9);
        assertEquals(1.0, projected.scale, 1e-9);
    }

    @Test
    void appliesXAxisAndYAxisCenteringBeforeProjection() {
        Grid grid = new Grid(3, 3, 3);
        Projection3D projection = new Projection3D(grid, 1.0, 0.0, 0.0, 800, 600);

        ProjectedPoint projected = projection.project(2, 1, 1);

        assertEquals(462, projected.screenX);
        assertEquals(300, projected.screenY);
        assertEquals(0.0, projected.depth, 1e-9);
    }

    @Test
    void rotationAroundYChangesDepthAndScreenPosition() {
        Grid grid = new Grid(3, 3, 3);
        Projection3D projection = new Projection3D(grid, 1.0, 0.0, Math.PI / 2, 800, 600);

        ProjectedPoint projected = projection.project(2, 1, 1);

        assertEquals(400, projected.screenX);
        assertEquals(300, projected.screenY);
        assertEquals(-62.0, projected.depth, 1e-9);
        assertEquals(850.0 / 788.0, projected.scale, 1e-9);
    }

    @Test
    void smallerVisualScaleChangesProjectedSizeButNotCentering() {
        Grid grid = new Grid(1, 1, 1);
        Projection3D projection = new Projection3D(grid, 0.5, 0.0, 0.0, 800, 600);

        ProjectedPoint projected = projection.project(0, 0, 0);

        assertEquals(400, projected.screenX);
        assertEquals(300, projected.screenY);
        assertEquals(1.0, projected.scale, 1e-9);
    }
}
