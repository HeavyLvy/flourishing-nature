package io.github.heavylvy.flourishingnature.core.land;

import io.github.heavylvy.flourishingnature.core.random.Randomness;

/** A range of game ticks, both ends inclusive. */
public record TickSpan(int min, int max) {
    public TickSpan {
        if (min < 0 || max < min) {
            throw new IllegalArgumentException("bad tick span: " + min + ".." + max);
        }
    }

    public int roll(Randomness random) {
        return random.nextIntInclusive(min, max);
    }
}
