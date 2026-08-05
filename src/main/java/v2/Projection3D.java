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
    private final boolean thirdPerson;
    private double cameraX;
    private double cameraY;
    private double cameraZ;
    private double rightX;
    private double rightY;
    private double rightZ;
    private double upX;
    private double upY;
    private double upZ;
    private double forwardX;
    private double forwardY;
    private double forwardZ;
    private double followDistance;

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
        this.thirdPerson = false;
    }

    private Projection3D(
            Grid grid,
            double visualScale,
            double focusX,
            double focusY,
            double focusZ,
            double headingX,
            double headingZ,
            int panelWidth,
            int panelHeight
    ) {
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.centerX = (grid.width - 1) / 2.0;
        this.centerY = (grid.height - 1) / 2.0;
        this.centerZ = (grid.depth - 1) / 2.0;
        this.spacing = 62 * visualScale;
        this.cosY = 1;
        this.sinY = 0;
        this.cosX = 1;
        this.sinX = 0;
        this.thirdPerson = true;

        double headingLength = Math.hypot(headingX, headingZ);
        double normalizedHeadingX = headingLength < 0.0001 ? 0 : headingX / headingLength;
        double normalizedHeadingZ = headingLength < 0.0001 ? 1 : headingZ / headingLength;

        double behindDistance = 4.0;
        double cameraHeight = behindDistance * Math.tan(Math.toRadians(30));
        cameraX = focusX - normalizedHeadingX * behindDistance;
        cameraY = focusY - cameraHeight;
        cameraZ = focusZ - normalizedHeadingZ * behindDistance;

        double viewX = focusX - cameraX;
        double viewY = focusY - cameraY;
        double viewZ = focusZ - cameraZ;
        followDistance = Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
        forwardX = viewX / followDistance;
        forwardY = viewY / followDistance;
        forwardZ = viewZ / followDistance;

        // Cross(forward, world-up), where world-up is negative Y in the grid coordinate system.
        rightX = forwardZ;
        rightY = 0;
        rightZ = -forwardX;
        double rightLength = Math.hypot(rightX, rightZ);
        if (rightLength < 0.0001) {
            rightX = 1;
            rightZ = 0;
        } else {
            rightX /= rightLength;
            rightZ /= rightLength;
        }

        // Cross(right, forward) produces the camera's upward screen axis.
        upX = rightY * forwardZ - rightZ * forwardY;
        upY = rightZ * forwardX - rightX * forwardZ;
        upZ = rightX * forwardY - rightY * forwardX;
    }

    public static Projection3D thirdPerson(
            Grid grid,
            double visualScale,
            double focusX,
            double focusY,
            double focusZ,
            double headingX,
            double headingZ,
            int panelWidth,
            int panelHeight
    ) {
        return new Projection3D(
                grid,
                visualScale,
                focusX,
                focusY,
                focusZ,
                headingX,
                headingZ,
                panelWidth,
                panelHeight
        );
    }

    public ProjectedPoint project(double x, double y, double z) {
        if (thirdPerson) {
            return projectThirdPerson(x, y, z);
        }

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

    public boolean isVisible(double x, double y, double z) {
        if (!thirdPerson) return true;

        double relativeX = x - cameraX;
        double relativeY = y - cameraY;
        double relativeZ = z - cameraZ;
        double cameraSpaceZ = relativeX * forwardX + relativeY * forwardY + relativeZ * forwardZ;
        return cameraSpaceZ > 0.25;
    }

    private ProjectedPoint projectThirdPerson(double x, double y, double z) {
        double relativeX = x - cameraX;
        double relativeY = y - cameraY;
        double relativeZ = z - cameraZ;

        double cameraSpaceX = relativeX * rightX + relativeY * rightY + relativeZ * rightZ;
        double cameraSpaceY = relativeX * upX + relativeY * upY + relativeZ * upZ;
        double cameraSpaceZ = relativeX * forwardX + relativeY * forwardY + relativeZ * forwardZ;
        if (cameraSpaceZ <= 0.25) {
            return new ProjectedPoint(-100_000, -100_000, -cameraSpaceZ, 0.01);
        }
        double safeDepth = Math.max(0.15, cameraSpaceZ);
        double perspective = followDistance / safeDepth;

        int screenX = (int) (panelWidth / 2.0 + cameraSpaceX * spacing * perspective);
        int screenY = (int) (panelHeight / 2.0 - cameraSpaceY * spacing * perspective);

        return new ProjectedPoint(screenX, screenY, -cameraSpaceZ, perspective);
    }
}
