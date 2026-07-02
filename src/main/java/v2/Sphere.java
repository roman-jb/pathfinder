package v2;

import java.awt.Color;

public class Sphere {
    public final Point3D point;
    public final ProjectedPoint projected;
    public final Color color;
    public final String label;

    public Sphere(Point3D point, ProjectedPoint projected, Color color, String label) {
        this.point = point;
        this.projected = projected;
        this.color = color;
        this.label = label;
    }
}
