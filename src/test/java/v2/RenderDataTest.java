package v2;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RenderDataTest {

    @Test
    void constructorCopiesPathIntoImmutableSet() {
        Grid grid = new Grid(2, 2, 1);
        List<Point3D> path = new ArrayList<>(List.of(
                new Point3D(0, 0, 0),
                new Point3D(1, 0, 0)
        ));

        RenderData data = new RenderData(
                grid,
                PathType.SHORTEST,
                path.get(0),
                path.get(1),
                path,
                Set.of(),
                null,
                false,
                false
        );

        assertTrue(data.pathSet.contains(new Point3D(0, 0, 0)));
        assertTrue(data.pathSet.contains(new Point3D(1, 0, 0)));

        path.add(new Point3D(1, 1, 0));

        assertFalse(data.pathSet.contains(new Point3D(1, 1, 0)));
        assertEquals(Set.of(
                new Point3D(0, 0, 0),
                new Point3D(1, 0, 0)
        ), data.pathSet);
    }

    @Test
    void constructorUsesEmptySetWhenPathIsNull() {
        RenderData data = new RenderData(
                new Grid(1, 1, 1),
                PathType.SHORTEST,
                null,
                null,
                null,
                null,
                null,
                false,
                false
        );

        assertTrue(data.pathSet.isEmpty());
        assertTrue(data.interactiveFrontier.isEmpty());
    }

    @Test
    void interactiveFrontierIsDefensivelyCopied() {
        Set<Point3D> frontier = new HashSet<>(Set.of(new Point3D(1, 0, 0)));

        RenderData data = new RenderData(
                new Grid(2, 1, 1),
                PathType.INTERACTIVE,
                new Point3D(0, 0, 0),
                new Point3D(1, 0, 0),
                List.of(new Point3D(0, 0, 0)),
                frontier,
                null,
                false,
                true
        );

        assertTrue(data.interactiveFrontier.contains(new Point3D(1, 0, 0)));
        frontier.add(new Point3D(0, 0, 0));
        assertFalse(data.interactiveFrontier.contains(new Point3D(0, 0, 0)));
        assertEquals(Set.of(new Point3D(1, 0, 0)), data.interactiveFrontier);
    }
}
