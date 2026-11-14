package io.github.heavylvy.flourishingnature.core.nest;

/**
 * Nest material a hen has gathered so far, in points (a stick is 1, a tuft of tall grass 2).
 *
 * @param value points gathered, never negative
 */
public record BuildPoints(int value) {
    public static final BuildPoints NONE = new BuildPoints(0);

    public BuildPoints {
        if (value < 0) {
            throw new IllegalArgumentException("build points must be >= 0: " + value);
        }
    }

    public BuildPoints plus(int points) {
        return new BuildPoints(value + points);
    }

    public boolean enoughFor(NestSettings settings) {
        return value >= settings.buildPoints();
    }
}
