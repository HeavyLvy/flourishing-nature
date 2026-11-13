package io.github.heavylvy.flourishingnature.core.herd;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FleeRouteTest {
    private static final Position SELF = new Position(0, 64, 0);
    private static final Position DANGER = new Position(-5, 64, 0);

    @Test
    void alwaysRunsAwayFromTheDanger() {
        SeededRandomness random = new SeededRandomness(7);
        for (int i = 0; i < 1_000; i++) {
            Position target = FleeRoute.pick(SELF, DANGER, random);
            assertTrue(target.distanceTo(DANGER) > SELF.distanceTo(DANGER), "ran toward the danger: " + target);
            double run = target.distanceTo(SELF);
            assertTrue(run >= FleeRoute.MIN_DISTANCE - 1.0e-9 && run <= FleeRoute.MAX_DISTANCE + 1.0e-9, "run " + run);
        }
    }

    @Test
    void herdMatesScatterInDifferentDirections() {
        SeededRandomness random = new SeededRandomness(8);
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (int i = 0; i < 50; i++) {
            Position target = FleeRoute.pick(SELF, DANGER, random);
            minZ = Math.min(minZ, target.z());
            maxZ = Math.max(maxZ, target.z());
        }
        assertTrue(minZ < -4 && maxZ > 4, "flee targets did not spread out: z from " + minZ + " to " + maxZ);
    }
}
