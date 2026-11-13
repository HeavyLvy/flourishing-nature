package io.github.heavylvy.flourishingnature.core.wellbeing;

/**
 * An animal's overall happiness.
 *
 * @param value in [0, 100]
 */
public record Wellbeing(double value) {
    public static final double MAX = 100.0;

    public Wellbeing {
        if (!(value >= 0.0 && value <= MAX)) {
            throw new IllegalArgumentException("wellbeing must be in [0, 100]: " + value);
        }
    }

    public Mood mood() {
        return Mood.of(this);
    }

    /** Moves toward {@code target} by at most {@code maxStep} points. */
    public Wellbeing approach(Wellbeing target, double maxStep) {
        if (!(maxStep >= 0.0)) {
            throw new IllegalArgumentException("step must be 0 or more: " + maxStep);
        }
        double delta = target.value - value;
        double step = Math.max(-maxStep, Math.min(maxStep, delta));
        return new Wellbeing(value + step);
    }
}
