package v2;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Renderer2DTest {

    @Test
    void drawsStandard2DViewWithoutThrowingAndChangesPixels() {
        Grid grid = new Grid(2, 2, 1);
        fillWeights(grid);

        RenderData data = new RenderData(
                grid,
                PathType.SHORTEST,
                new Point3D(0, 0, 0),
                new Point3D(1, 1, 0),
                List.of(new Point3D(0, 0, 0), new Point3D(1, 1, 0)),
                Set.of(),
                null,
                false,
                false
        );

        BufferedImage image = new BufferedImage(500, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());

        assertDoesNotThrow(() -> new Renderer2D().draw(g2, data, 1.0));
        g2.dispose();

        assertTrue(countNonWhitePixels(image) > 0);
    }

    @Test
    void drawsInteractive2DViewWithoutThrowingAndChangesPixels() {
        Grid grid = new Grid(3, 3, 1);
        fillWeights(grid);

        Point3D start = new Point3D(1, 1, 0);
        Point3D frontier = new Point3D(2, 1, 0);

        RenderData data = new RenderData(
                grid,
                PathType.INTERACTIVE,
                start,
                new Point3D(2, 2, 0),
                List.of(start),
                Set.of(frontier),
                frontier,
                false,
                false
        );

        BufferedImage image = new BufferedImage(500, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());

        assertDoesNotThrow(() -> new Renderer2D().draw(g2, data, 1.0));
        g2.dispose();

        assertTrue(countNonWhitePixels(image) > 0);
    }

    private static void fillWeights(Grid grid) {
        for (int z = 0; z < grid.depth; z++) {
            for (int y = 0; y < grid.height; y++) {
                for (int x = 0; x < grid.width; x++) {
                    grid.weights[z][y][x] = 1;
                }
            }
        }
    }

    private static long countNonWhitePixels(BufferedImage image) {
        long count = 0;
        int white = Color.WHITE.getRGB();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != white) {
                    count++;
                }
            }
        }

        return count;
    }
}
