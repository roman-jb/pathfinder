package v2;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RenderData {
    public final Grid grid;
    public final PathType pathType;
    public final Point3D start;
    public final Point3D end;
    public final List<Point3D> path;
    public final Set<Point3D> interactiveFrontier;
    public final Point3D interactiveSelected;
    public final boolean interactiveComplete;
    public final boolean hideUnusedNodes;

    public RenderData(
            Grid grid,
            PathType pathType,
            Point3D start,
            Point3D end,
            List<Point3D> path,
            Set<Point3D> interactiveFrontier,
            Point3D interactiveSelected,
            boolean interactiveComplete,
            boolean hideUnusedNodes
    ) {
        this.grid = grid;
        this.pathType = pathType;
        this.start = start;
        this.end = end;
        this.path = path;
        this.interactiveFrontier = interactiveFrontier == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(interactiveFrontier));
        this.interactiveSelected = interactiveSelected;
        this.interactiveComplete = interactiveComplete;
        this.hideUnusedNodes = hideUnusedNodes;
    }
}
