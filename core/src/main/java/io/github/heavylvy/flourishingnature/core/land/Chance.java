package io.github.heavylvy.flourishingnature.core.land;

import io.github.heavylvy.flourishingnature.core.random.Randomness;

/** A probability in [0, 1] that something happens on one roll. */
public record Chance(double value) {
    public static final Chance NEVER = new Chance(0.0);
    public static final Chance ALWAYS = new Chance(1.0);

    public Chance {
        if (!(value >= 0.0 && value <= 1.0)) {
            throw new IllegalArgumentException("chance must be in [0, 1]: " + value);
        }
    }

    public boolean roll(Randomness random) {
        return value > 0.0 && random.nextDouble() < value;
    }

    /** This chance multiplied by a non-negative factor, capped at certainty. */
    public Chance scaled(double factor) {
        if (!(factor >= 0.0)) {
            throw new IllegalArgumentException("factor must be >= 0: " + factor);
        }
        return new Chance(Math.min(1.0, value * factor));
    }

    /** The chance that at least one of two independent rolls succeeds: rain's extra growth roll. */
    public Chance twice() {
        return new Chance(1.0 - (1.0 - value) * (1.0 - value));
    }
}
