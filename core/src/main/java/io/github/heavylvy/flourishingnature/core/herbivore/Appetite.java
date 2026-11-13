package io.github.heavylvy.flourishingnature.core.herbivore;

/**
 * How much an animal likes a food. A food with appetite 2 is worth walking twice as far for as
 * one with appetite 1.
 *
 * @param value above 0
 */
public record Appetite(double value) {
    public static final Appetite NORMAL = new Appetite(1.0);
    public static final Appetite FAVOURITE = new Appetite(2.0);

    public Appetite {
        if (!(value > 0.0) || !Double.isFinite(value)) {
            throw new IllegalArgumentException("appetite must be positive: " + value);
        }
    }
}
