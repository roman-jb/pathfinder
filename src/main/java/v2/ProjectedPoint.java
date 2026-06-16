package v2;

public class ProjectedPoint {
    public final int screenX;
    public final int screenY;
    public final double depth;
    public final double scale;

    public ProjectedPoint(int screenX, int screenY, double depth, double scale) {
        this.screenX = screenX;
        this.screenY = screenY;
        this.depth = depth;
        this.scale = scale;
    }
}
