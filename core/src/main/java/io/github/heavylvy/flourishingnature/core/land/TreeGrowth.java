package io.github.heavylvy.flourishingnature.core.land;

/**
 * How readily a biome's ungrazed land turns back into woodland, relative to a forest (1.0).
 * Plains are 0.1 (ten times slower), deserts 0 (never).
 */
public record TreeGrowth(double rate) {
    public static final double MAX = 10.0;
    public static final TreeGrowth NONE = new TreeGrowth(0.0);
    public static final TreeGrowth FOREST = new TreeGrowth(1.0);

    public TreeGrowth {
        if (!(rate >= 0.0 && rate <= MAX)) {
            throw new IllegalArgumentException("tree growth must be in [0, " + MAX + "]: " + rate);
        }
    }

    public boolean grows() {
        return rate > 0.0;
    }
}
