package v2;

public class Projection3D {

    private final int panelWidth;
    private final int panelHeight;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final double spacing;
    private final double cosY;
    private final double sinY;
    private final double cosX;
    private final double sinX;

    public Projection3D(
            Grid grid,
            double visualScale,
            double angleX,
            double angleY,
            int panelWidth,
            int panelHeight
    ) {
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.centerX = (grid.width - 1) / 2.0;
        this.centerY = (grid.height - 1) / 2.0;
        this.centerZ = (grid.depth - 1) / 2.0;
        this.spacing = 62 * visualScale;
        this.cosY = Math.cos(angleY);
        this.sinY = Math.sin(angleY);
        this.cosX = Math.cos(angleX);
        this.sinX = Math.sin(angleX);
    }

    public ProjectedPoint project(double x, double y, double z) {
        x -= centerX;
        y -= centerY;
        z -= centerZ;

        x *= spacing;
        y *= spacing;
        z *= spacing;

        double rotatedX = x * cosY + z * sinY;
        double rotatedZ = -x * sinY + z * cosY;

        double rotatedY = y * cosX - rotatedZ * sinX;
        rotatedZ = y * sinX + rotatedZ * cosX;

        double cameraDistance = 850;
        double scale = cameraDistance / (cameraDistance + rotatedZ);

        int screenX = (int) (panelWidth / 2.0 + rotatedX * scale);
        int screenY = (int) (panelHeight / 2.0 + rotatedY * scale);

        return new ProjectedPoint(screenX, screenY, rotatedZ, scale);
    }
}
