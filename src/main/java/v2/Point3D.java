package v2;

public record Point3D(int x, int y, int z) {

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Point3D(int x1, int y1, int z1))) return false;
        return x == x1 && y == y1 && z == z1;
    }

}
