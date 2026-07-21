package v2;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GridTest {

    @Test
    void isInsideRespectsBounds() {
        Grid grid = new Grid(3, 4, 2);

        assertTrue(grid.isInside(0, 0, 0));
        assertTrue(grid.isInside(2, 3, 1));
        assertFalse(grid.isInside(-1, 0, 0));
        assertFalse(grid.isInside(0, 4, 0));
        assertFalse(grid.isInside(0, 0, 2));
    }

    @Test
    void randomPointAlwaysFallsInsideGrid() {
        Grid grid = new Grid(5, 6, 7);

        for (int i = 0; i < 200; i++) {
            Point3D point = grid.randomPoint();

            assertTrue(grid.isInside(point.x, point.y, point.z));
        }
    }

    @Test
    void randomizeWeightsProducesValuesInExpectedRange() {
        Grid grid = new Grid(4, 4, 4);
        grid.randomizeWeights();

        for (int z = 0; z < grid.depth; z++) {
            for (int y = 0; y < grid.height; y++) {
                for (int x = 0; x < grid.width; x++) {
                    int weight = grid.weights[z][y][x];
                    assertTrue(weight >= 1 && weight <= 9, "weight=" + weight);
                }
            }
        }
    }

    @Test
    void calculatePathCostSumsEnteredCellsOnly() {
        Grid grid = new Grid(3, 1, 1);
        grid.weights[0][0][0] = 1;
        grid.weights[0][0][1] = 4;
        grid.weights[0][0][2] = 7;

        List<Point3D> path = List.of(
                new Point3D(0, 0, 0),
                new Point3D(1, 0, 0),
                new Point3D(2, 0, 0)
        );

        assertEquals(11, grid.calculatePathCost(path));
    }

    @Test
    void getWeightReadsFromCoordinates() {
        Grid grid = new Grid(2, 2, 2);
        grid.weights[1][0][1] = 8;

        assertEquals(8, grid.getWeight(new Point3D(1, 0, 1)));
    }
}
