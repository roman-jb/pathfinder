package v2;

import java.awt.Color;

public class Sphere {
    public final Point3D point;
    public final ProjectedPoint projected;
    public final Color color;

    public Sphere(Point3D point, ProjectedPoint projected) {
        this(point, projected, null);
    }

    public Sphere(Point3D point, ProjectedPoint projected, Color color) {
        this.point = point;
        this.projected = projected;
        this.color = color;
    }
}
