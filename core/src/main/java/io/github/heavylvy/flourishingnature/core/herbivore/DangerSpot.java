package io.github.heavylvy.flourishingnature.core.herbivore;

import io.github.heavylvy.flourishingnature.core.herd.Position;

/**
 * Where and when an animal was attacked.
 *
 * @param where the spot
 * @param atTick game tick of the attack
 */
public record DangerSpot(Position where, long atTick) {

    /**
     * How far around the spot the herd keeps away, now: the full radius for the first day, then
     * shrinking to nothing over the next.
     */
    public double radius(long now, DangerSettings settings) {
        long age = Math.max(0L, now - atTick);
        if (age <= settings.holdTicks()) {
            return settings.radius();
        }
        double faded = (double) (age - settings.holdTicks()) / settings.fadeTicks();
        return Math.max(0.0, settings.radius() * (1.0 - faded));
    }

    public boolean forgotten(long now, DangerSettings settings) {
        return now - atTick >= settings.holdTicks() + settings.fadeTicks();
    }

    /** Whether {@code p} is inside the area the herd avoids, now. */
    public boolean covers(Position p, long now, DangerSettings settings) {
        double r = radius(now, settings);
        return r > 0.0 && horizontalDistance(p) < r;
    }

    double horizontalDistance(Position p) {
        double dx = p.x() - where.x();
        double dz = p.z() - where.z();
        return Math.sqrt(dx * dx + dz * dz);
    }
}
