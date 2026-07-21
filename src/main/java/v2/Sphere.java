package v2;

import java.awt.Color;

public record Sphere(Point3D point, ProjectedPoint projected, Color color, String label, double sizeFactor) {
}
