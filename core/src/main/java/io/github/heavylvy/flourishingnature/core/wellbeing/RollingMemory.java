package io.github.heavylvy.flourishingnature.core.wellbeing;

/**
 * A short memory of a yes/no condition, so stepping under a tree for a moment doesn't count as
 * living in a cave. Each sample nudges the value toward 1 (yes) or 0 (no).
 *
 * @param value in [0, 1]
 */
public record RollingMemory(double value) {
    public static final RollingMemory ALWAYS = new RollingMemory(1.0);

    public RollingMemory {
        if (!(value >= 0.0 && value <= 1.0)) {
            throw new IllegalArgumentException("memory must be in [0, 1]: " + value);
        }
    }

    /** {@code weight} in (0, 1] is how much the new sample counts. */
    public RollingMemory remember(boolean sample, double weight) {
        if (!(weight > 0.0 && weight <= 1.0)) {
            throw new IllegalArgumentException("sample weight must be in (0, 1]: " + weight);
        }
        double target = sample ? 1.0 : 0.0;
        return new RollingMemory(Math.max(0.0, Math.min(1.0, value + (target - value) * weight)));
    }
}
