package io.github.heavylvy.flourishingnature.core.herd;

import java.util.Collection;

/** A point in the world, in blocks. */
public record Position(double x, double y, double z) {
    public Position {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("position must be finite: " + x + ", " + y + ", " + z);
        }
    }

    public double distanceTo(Position other) {
        return Math.sqrt(distanceSquaredTo(other));
    }

    public double distanceSquaredTo(Position other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * The point {@code distance} blocks from here, heading directly away from {@code from} on the
     * horizontal plane. If the two points share x and z, heads toward +x.
     */
    public Position awayFrom(Position from, double distance) {
        double dx = x - from.x;
        double dz = z - from.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0e-6) {
            return new Position(x + distance, y, z);
        }
        return new Position(x + dx / length * distance, y, z + dz / length * distance);
    }

    /** Average of the given points weighted equally. The collection must not be empty. */
    public static Position centroid(Collection<Position> points) {
        if (points.isEmpty()) {
            throw new IllegalArgumentException("centroid of no points");
        }
        double sx = 0;
        double sy = 0;
        double sz = 0;
        for (Position p : points) {
            sx += p.x;
            sy += p.y;
            sz += p.z;
        }
        int n = points.size();
        return new Position(sx / n, sy / n, sz / n);
    }
}
