package io.github.heavylvy.flourishingnature.core.predator;

import io.github.heavylvy.flourishingnature.core.herd.Position;

/**
 * A wolf that left its starving pack and travels alone toward new territory.
 *
 * @param target where it is heading
 * @param until game tick it gives up and settles wherever it is
 */
public record Dispersal(Position target, long until) {
    public static final double ARRIVED_WITHIN = 6.0;

    public boolean active(long now, Position here) {
        return now < until && horizontal(here) > ARRIVED_WITHIN;
    }

    private double horizontal(Position here) {
        double dx = target.x() - here.x();
        double dz = target.z() - here.z();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
