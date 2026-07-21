package v2;

import java.util.*;

public class Pathfinder {

    private record Node(Point3D point, int cost) {
    }

    public static List<Point3D> findPath(
            Grid grid,
            Point3D start,
            Point3D end,
            PathType pathType
    ) {
        int[][][] dist = new int[grid.depth][grid.height][grid.width];
        Point3D[][][] previous = new Point3D[grid.depth][grid.height][grid.width];

        for (int z = 0; z < grid.depth; z++) {
            for (int y = 0; y < grid.height; y++) {
                Arrays.fill(dist[z][y], Integer.MAX_VALUE);
            }
        }

        PriorityQueue<Node> queue = new PriorityQueue<>(
                Comparator.comparingInt(n -> n.cost)
        );

        dist[start.z()][start.y()][start.x()] = 0;
        queue.add(new Node(start, 0));

        int[][] directions = grid.depth == 1
                ? new int[][]{
                {1, 0, 0},
                {-1, 0, 0},
                {0, 1, 0},
                {0, -1, 0}
        }
                : new int[][]{
                {1, 0, 0},
                {-1, 0, 0},
                {0, 1, 0},
                {0, -1, 0},
                {0, 0, 1},
                {0, 0, -1}
        };

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            Point3D p = current.point;

            if (p.equals(end)) break;

            if (current.cost > dist[p.z()][p.y()][p.x()]) continue;

            for (int[] d : directions) {
                int nx = p.x() + d[0];
                int ny = p.y() + d[1];
                int nz = p.z() + d[2];

                if (!grid.isInside(nx, ny, nz)) continue;

                Point3D next = new Point3D(nx, ny, nz);

                int stepCost = pathType == PathType.CHEAPEST
                        ? grid.getWeight(next)
                        : 1;

                int newCost = dist[p.z()][p.y()][p.x()] + stepCost;

                if (newCost < dist[nz][ny][nx]) {
                    dist[nz][ny][nx] = newCost;
                    previous[nz][ny][nx] = p;
                    queue.add(new Node(next, newCost));
                }
            }
        }

        return restorePath(previous, start, end);
    }

    private static List<Point3D> restorePath(
            Point3D[][][] previous,
            Point3D start,
            Point3D end
    ) {
        List<Point3D> path = new ArrayList<>();

        Point3D current = end;

        while (current != null) {
            path.add(current);

            if (current.equals(start)) break;

            current = previous[current.z()][current.y()][current.x()];
        }

        Collections.reverse(path);

        if (path.isEmpty() || !path.getFirst().equals(start)) {
            return Collections.emptyList();
        }

        return path;
    }
}
