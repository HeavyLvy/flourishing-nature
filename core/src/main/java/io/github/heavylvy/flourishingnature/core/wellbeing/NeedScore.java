package io.github.heavylvy.flourishingnature.core.wellbeing;

/**
 * How well one need is met: 0 is not at all, 1 is fully.
 *
 * @param value in [0, 1]
 */
public record NeedScore(double value) {
    public static final NeedScore FULL = new NeedScore(1.0);
    public static final NeedScore EMPTY = new NeedScore(0.0);

    public NeedScore {
        if (!(value >= 0.0 && value <= 1.0)) {
            throw new IllegalArgumentException("need score must be in [0, 1]: " + value);
        }
    }

    public static NeedScore clamped(double value) {
        if (Double.isNaN(value)) {
            throw new IllegalArgumentException("need score is NaN");
        }
        return new NeedScore(Math.max(0.0, Math.min(1.0, value)));
    }
}
