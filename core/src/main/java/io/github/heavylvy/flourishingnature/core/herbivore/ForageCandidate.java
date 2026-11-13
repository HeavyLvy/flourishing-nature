package io.github.heavylvy.flourishingnature.core.herbivore;

/**
 * One food an animal could walk to.
 *
 * @param food what it is
 * @param distance how far away, in blocks
 */
public record ForageCandidate(Food food, double distance) {
    public ForageCandidate {
        if (!(distance >= 0.0) || !Double.isFinite(distance)) {
            throw new IllegalArgumentException("distance must be 0 or more: " + distance);
        }
    }
}
