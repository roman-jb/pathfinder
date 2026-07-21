package v2;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathfinderTest {

    @Test
    void shortestPathIn2DUsesFewestSteps() {
        Grid grid = new Grid(3, 3, 1);
        fillWeights(grid, 1);

        Point3D start = new Point3D(0, 0, 0);
        Point3D end = new Point3D(2, 0, 0);

        List<Point3D> path = Pathfinder.findPath(grid, start, end, PathType.SHORTEST);

        assertEquals(List.of(
                new Point3D(0, 0, 0),
                new Point3D(1, 0, 0),
                new Point3D(2, 0, 0)
        ), path);
    }

    @Test
    void cheapestPathIn2DAvoidsHighCostCells() {
        Grid grid = new Grid(3, 3, 1);
        fillWeights(grid, 9);

        grid.weights[0][1][0] = 1;
        grid.weights[0][1][1] = 1;
        grid.weights[0][1][2] = 1;
        grid.weights[0][0][2] = 9;
        grid.weights[0][0][1] = 9;

        Point3D start = new Point3D(0, 0, 0);
        Point3D end = new Point3D(2, 0, 0);

        List<Point3D> path = Pathfinder.findPath(grid, start, end, PathType.CHEAPEST);

        assertEquals(List.of(
                new Point3D(0, 0, 0),
                new Point3D(0, 1, 0),
                new Point3D(1, 1, 0),
                new Point3D(2, 1, 0),
                new Point3D(2, 0, 0)
        ), path);
        assertEquals(12, grid.calculatePathCost(path));
    }

    @Test
    void shortestPathIn3DUsesOrthogonalMovesOnly() {
        Grid grid = new Grid(2, 2, 2);
        fillWeights(grid, 1);

        Point3D start = new Point3D(0, 0, 0);
        Point3D end = new Point3D(1, 1, 1);

        List<Point3D> path = Pathfinder.findPath(grid, start, end, PathType.SHORTEST);

        assertEquals(start, path.getFirst());
        assertEquals(end, path.getLast());
        assertEquals(4, path.size());

        for (int i = 1; i < path.size(); i++) {
            Point3D a = path.get(i - 1);
            Point3D b = path.get(i);
            int manhattan = Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y()) + Math.abs(a.z() - b.z());
            assertEquals(1, manhattan);
        }
    }

    private static void fillWeights(Grid grid, int value) {
        for (int z = 0; z < grid.depth; z++) {
            for (int y = 0; y < grid.height; y++) {
                for (int x = 0; x < grid.width; x++) {
                    grid.weights[z][y][x] = value;
                }
            }
        }
    }
}
