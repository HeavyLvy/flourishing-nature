package io.github.heavylvy.flourishingnature.core.predator;

import io.github.heavylvy.flourishingnature.core.herd.Position;

/**
 * An animal a wolf could go after.
 *
 * @param handle the game's entity id, so the caller can find the animal again
 * @param position where it stands
 * @param baby whether it is young
 * @param health current health as a share of its maximum, in [0, 1]
 */
public record Prey(int handle, Position position, boolean baby, double health) {
    public Prey {
        if (!(health >= 0.0 && health <= 1.0)) {
            throw new IllegalArgumentException("health share must be in [0, 1]: " + health);
        }
    }

    /** 0 for a healthy adult, up to 2 for a badly hurt baby. */
    public double weakness() {
        return (baby ? 1.0 : 0.0) + (1.0 - health);
    }
}
