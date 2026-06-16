package v2;

import java.awt.*;
import java.util.Set;

public class RenderUtils {

    public static Color getCellColor(RenderData data, Point3D p, Set<Point3D> pathSet) {
        if (p.equals(data.start)) {
            return new Color(70, 190, 90);
        }

        if (p.equals(data.end)) {
            return new Color(230, 75, 75);
        }

        if (pathSet.contains(p)) {
            return new Color(75, 145, 235);
        }

        int weight = data.grid.weights[p.z][p.y][p.x];
        int shade = 245 - weight * 14;

        return new Color(shade, shade, shade);
    }

    public static String getCellText(RenderData data, Point3D p) {
        if (p.equals(data.start)) return "S";
        if (p.equals(data.end)) return "E";

        return String.valueOf(data.grid.weights[p.z][p.y][p.x]);
    }

    public static Color darken(Color color, double factor) {
        return new Color(
                Math.max(0, (int) (color.getRed() * factor)),
                Math.max(0, (int) (color.getGreen() * factor)),
                Math.max(0, (int) (color.getBlue() * factor))
        );
    }

    public static Color brighten(Color color, double factor) {
        return new Color(
                Math.min(255, (int) (color.getRed() * factor)),
                Math.min(255, (int) (color.getGreen() * factor)),
                Math.min(255, (int) (color.getBlue() * factor))
        );
    }
}
