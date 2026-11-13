package io.github.heavylvy.flourishingnature.core.herd;

/**
 * An inclusive range of herd sizes.
 *
 * @param min lowest value, at least {@link HerdPreference#MIN}
 * @param max highest value, at most {@link HerdPreference#MAX}
 */
public record IntRange(int min, int max) {
    public IntRange {
        if (min < HerdPreference.MIN || max > HerdPreference.MAX || min > max) {
            throw new IllegalArgumentException("bad herd size range [" + min + ", " + max + "]");
        }
    }

    public static IntRange of(int min, int max) {
        return new IntRange(min, max);
    }

    public static IntRange exactly(int value) {
        return new IntRange(value, value);
    }
}
