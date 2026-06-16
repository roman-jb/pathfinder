package v2;

public class Sphere {
    public final Point3D point;
    public final ProjectedPoint projected;

    public Sphere(Point3D point, ProjectedPoint projected) {
        this.point = point;
        this.projected = projected;
    }
}
