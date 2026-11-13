package io.github.heavylvy.flourishingnature.core.jobs;

/** A block position, for remembering where an animal worked. */
public record BlockSpot(int x, int y, int z) {
    /** Horizontal and vertical distance both within {@code radius} (a box, not a sphere). */
    public boolean within(BlockSpot other, int radius) {
        return Math.abs(x - other.x) <= radius && Math.abs(y - other.y) <= radius && Math.abs(z - other.z) <= radius;
    }
}
