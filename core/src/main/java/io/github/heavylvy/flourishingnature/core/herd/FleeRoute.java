package io.github.heavylvy.flourishingnature.core.herd;

import io.github.heavylvy.flourishingnature.core.random.Randomness;

/**
 * Picks where a frightened animal runs: roughly away from the danger, but each animal swerves by a
 * random angle, so a herd bursts apart like a startled flock instead of running as one block.
 */
public final class FleeRoute {
    private FleeRoute() {}

    /** Up to this many radians either side of straight away from the danger. */
    static final double MAX_SWERVE = Math.PI / 3;
    static final double MIN_DISTANCE = 8.0;
    static final double MAX_DISTANCE = 12.0;

    public static Position pick(Position self, Position danger, Randomness random) {
        double dx = self.x() - danger.x();
        double dz = self.z() - danger.z();
        double heading = (dx * dx + dz * dz) < 1.0e-6
                ? random.nextDouble() * 2 * Math.PI
                : Math.atan2(dz, dx);
        double angle = heading + (random.nextDouble() * 2 - 1) * MAX_SWERVE;
        double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        return new Position(self.x() + Math.cos(angle) * distance, self.y(), self.z() + Math.sin(angle) * distance);
    }
}
