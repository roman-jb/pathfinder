package v2;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class Point3DTest {

    @Test
    void equalsAndHashCodeUseCoordinates() {
        Point3D a = new Point3D(1, 2, 3);
        Point3D b = new Point3D(1, 2, 3);
        Point3D c = new Point3D(3, 2, 1);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);

        HashSet<Point3D> set = new HashSet<>();
        set.add(a);

        assertTrue(set.contains(b));
        assertFalse(set.contains(c));
    }

    @Test
    void equalsRejectsNullAndOtherTypes() {
        Point3D point = new Point3D(1, 2, 3);

        assertNotEquals(null, point);
        assertNotEquals("1,2,3", point);
    }
}
