package io.github.heavylvy.flourishingnature.core.herd;

/**
 * How many animals, counting itself, a mob wants in its herd. Rolled once and kept for life.
 *
 * @param value 1 (a loner) up to {@link #MAX}
 */
public record HerdPreference(int value) {
    public static final int MIN = 1;
    public static final int MAX = 16;

    public HerdPreference {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException("herd preference must be in [" + MIN + ", " + MAX + "]: " + value);
        }
    }

    /** Herd mates wanted besides itself. A loner wants 0. */
    public int desiredMates() {
        return value - 1;
    }

    public static HerdPreference clamped(long value) {
        return new HerdPreference((int) Math.max(MIN, Math.min(MAX, value)));
    }
}
