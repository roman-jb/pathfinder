package v2;

public class Projection3D {

    private final Grid grid;
    private final double visualScale;
    private final double angleX;
    private final double angleY;
    private final int panelWidth;
    private final int panelHeight;

    public Projection3D(
            Grid grid,
            double visualScale,
            double angleX,
            double angleY,
            int panelWidth,
            int panelHeight
    ) {
        this.grid = grid;
        this.visualScale = visualScale;
        this.angleX = angleX;
        this.angleY = angleY;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }

    public ProjectedPoint project(double x, double y, double z) {
        double centerX = (grid.width - 1) / 2.0;
        double centerY = (grid.height - 1) / 2.0;
        double centerZ = (grid.depth - 1) / 2.0;

        x -= centerX;
        y -= centerY;
        z -= centerZ;

        double spacing = 62 * visualScale;

        x *= spacing;
        y *= spacing;
        z *= spacing;

        double cosY = Math.cos(angleY);
        double sinY = Math.sin(angleY);

        double rotatedX = x * cosY + z * sinY;
        double rotatedZ = -x * sinY + z * cosY;

        double cosX = Math.cos(angleX);
        double sinX = Math.sin(angleX);

        double rotatedY = y * cosX - rotatedZ * sinX;
        rotatedZ = y * sinX + rotatedZ * cosX;

        double cameraDistance = 850;
        double scale = cameraDistance / (cameraDistance + rotatedZ);

        int screenX = (int) (panelWidth / 2.0 + rotatedX * scale);
        int screenY = (int) (panelHeight / 2.0 + rotatedY * scale);

        return new ProjectedPoint(screenX, screenY, rotatedZ, scale);
    }
}
