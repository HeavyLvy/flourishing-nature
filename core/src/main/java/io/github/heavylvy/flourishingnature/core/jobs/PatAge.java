package io.github.heavylvy.flourishingnature.core.jobs;

import java.util.Optional;

/**
 * How far a cow pat has dried: 0 fresh, {@link #DRIED} dry. A dried pat crumbles on its next step.
 * Matches the block's {@code age} state.
 */
public record PatAge(int value) {
    public static final int DRIED = 3;
    public static final PatAge FRESH = new PatAge(0);

    public PatAge {
        if (value < 0 || value > DRIED) {
            throw new IllegalArgumentException("pat age must be 0.." + DRIED + ": " + value);
        }
    }

    /** Not yet dry: cows won't graze next to it. */
    public boolean fresh() {
        return value < DRIED;
    }

    public boolean dried() {
        return value == DRIED;
    }

    /** The next stage, or empty when a dried pat crumbles away. */
    public Optional<PatAge> next() {
        return dried() ? Optional.empty() : Optional.of(new PatAge(value + 1));
    }
}
