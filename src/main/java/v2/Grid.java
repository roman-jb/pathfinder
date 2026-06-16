package v2;

import java.util.List;
import java.util.Random;

public class Grid {
    public final int width;
    public final int height;
    public final int depth;
    public final int[][][] weights;

    private final Random random = new Random();

    public Grid(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.weights = new int[depth][height][width];
    }

    public void randomizeWeights() {
        for (int z = 0; z < depth; z++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    weights[z][y][x] = random.nextInt(9) + 1;
                }
            }
        }
    }

    public int getWeight(Point3D p) {
        return weights[p.z][p.y][p.x];
    }

    public int calculatePathCost(List<Point3D> path) {
        int cost = 0;

        for (int i = 1; i < path.size(); i++) {
            cost += getWeight(path.get(i));
        }

        return cost;
    }

    public boolean isInside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0
                && x < width && y < height && z < depth;
    }

    public Point3D randomPoint() {
        return new Point3D(
                random.nextInt(width),
                random.nextInt(height),
                random.nextInt(depth)
        );
    }
}
