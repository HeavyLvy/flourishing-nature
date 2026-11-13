package io.github.heavylvy.flourishingnature.core.herd;

/**
 * How much one species counts another as a herd mate. 1 is a full mate, 0 is ignored, negative
 * values (predators) will feed into stress later and never count toward a herd.
 *
 * @param value in [-1, 1]
 */
public record Affinity(double value) {
    public static final Affinity NONE = new Affinity(0.0);
    public static final Affinity SAME_SPECIES = new Affinity(1.0);

    public Affinity {
        if (!(value >= -1.0 && value <= 1.0)) {
            throw new IllegalArgumentException("affinity must be in [-1, 1]: " + value);
        }
    }

    public boolean isFriendly() {
        return value > 0.0;
    }
}
