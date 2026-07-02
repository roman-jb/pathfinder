package v2;

import java.awt.*;
import java.util.Set;

public class RenderUtils {

    public static Color getCellColor(RenderData data, Point3D p, Set<Point3D> pathSet) {
        if (data.pathType == PathType.INTERACTIVE) {
            if (p.equals(data.start)) {
                return new Color(70, 190, 90);
            }

            if (p.equals(data.end)) {
                return new Color(230, 75, 75);
            }

            if (p.equals(data.interactiveSelected)) {
                return new Color(70, 190, 90);
            }

            if (pathSet.contains(p)) {
                return new Color(75, 145, 235);
            }

            if (data.interactiveFrontier.contains(p)) {
                return new Color(70, 190, 90);
            }

            return new Color(120, 120, 120, 150);
        }

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
        if (data.pathType == PathType.INTERACTIVE) {
            if (p.equals(data.start)) return "S";
            if (p.equals(data.end)) return "E";
            if (p.equals(data.interactiveSelected)) return ">";
            if (pathContains(data, p)) return "";
            if (data.interactiveFrontier.contains(p)) {
                return String.valueOf(data.grid.weights[p.z][p.y][p.x]);
            }
            return "";
        }

        if (p.equals(data.start)) return "S";
        if (p.equals(data.end)) return "E";

        return String.valueOf(data.grid.weights[p.z][p.y][p.x]);
    }

    public static boolean isInteractiveVisible(RenderData data, Point3D p) {
        return isInteractiveSpecial(data, p) || !data.hideUnusedNodes;
    }

    public static boolean isSurfaceNode(Grid grid, Point3D p) {
        return p.x == 0
                || p.y == 0
                || p.z == 0
                || p.x == grid.width - 1
                || p.y == grid.height - 1
                || p.z == grid.depth - 1;
    }

    public static boolean isInteractiveBox(RenderData data, Point3D p) {
        if (data.pathType != PathType.INTERACTIVE) {
            return false;
        }

        return isSurfaceNode(data.grid, p) && isInteractiveVisible(data, p);
    }

    public static boolean isInteractiveSphere(RenderData data, Point3D p) {
        if (data.pathType != PathType.INTERACTIVE) {
            return false;
        }

        return !isSurfaceNode(data.grid, p) && isInteractiveVisible(data, p);
    }

    public static boolean isInteractiveSpecial(RenderData data, Point3D p) {
        return p.equals(data.start)
                || p.equals(data.end)
                || p.equals(data.interactiveSelected)
                || pathContains(data, p)
                || data.interactiveFrontier.contains(p);
    }

    public static double getInteractiveSizeFactor(RenderData data, Point3D p) {
        return isInteractiveSpecial(data, p) ? 1.0 : 0.5;
    }

    public static boolean pathContains(RenderData data, Point3D p) {
        return data.path.contains(p);
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
