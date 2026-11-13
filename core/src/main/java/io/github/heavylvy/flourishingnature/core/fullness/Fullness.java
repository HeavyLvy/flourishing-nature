package io.github.heavylvy.flourishingnature.core.fullness;

/**
 * How fed an animal is, from 0 (empty) to 1 (stuffed).
 *
 * @param value in [0, 1]
 */
public record Fullness(double value) {
    public static final Fullness FULL = new Fullness(1.0);
    public static final Fullness EMPTY = new Fullness(0.0);

    public Fullness {
        if (!(value >= 0.0 && value <= 1.0)) {
            throw new IllegalArgumentException("fullness must be in [0, 1]: " + value);
        }
    }

    public static Fullness clamped(double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("fullness is NaN");
        }
        return new Fullness(Math.max(0.0, Math.min(1.0, value)));
    }

    public Fullness plus(double amount) {
        return clamped(value + amount);
    }

    public HungerBand band() {
        return HungerBand.of(this);
    }
}
