package v2;

public class Cube {
    public final Point3D point;
    public final ProjectedPoint[] points;
    public final double depth;
    public final double sizeFactor;

    public Cube(Point3D point, ProjectedPoint[] points, double depth, double sizeFactor) {
        this.point = point;
        this.points = points;
        this.depth = depth;
        this.sizeFactor = sizeFactor;
    }
}
