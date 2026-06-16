package v2;

import java.util.List;

public class RenderData {
    public final Grid grid;
    public final Point3D start;
    public final Point3D end;
    public final List<Point3D> path;

    public RenderData(Grid grid, Point3D start, Point3D end, List<Point3D> path) {
        this.grid = grid;
        this.start = start;
        this.end = end;
        this.path = path;
    }
}
