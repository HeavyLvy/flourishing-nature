package io.github.heavylvy.flourishingnature.core.land;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class TramplingTest {
    @Test
    void groundWearsOneStageAtATime() {
        FixedRandomness hit = FixedRandomness.rolling(0.0);
        assertEquals(TrampleChange.GRASS_TO_DIRT, Trampling.step(Ground.GRASS_BLOCK, false, 0.1, TrampleSettings.ALWAYS, hit));
        assertEquals(TrampleChange.DIRT_TO_COARSE_DIRT, Trampling.step(Ground.DIRT, false, 0.1, TrampleSettings.ALWAYS, hit));
        assertEquals(TrampleChange.COARSE_DIRT_TO_PATH, Trampling.step(Ground.COARSE_DIRT, false, 0.1, TrampleSettings.ALWAYS, hit));
        assertEquals(TrampleChange.NONE, Trampling.step(Ground.DIRT_PATH, false, 0.1, TrampleSettings.ALWAYS, hit));
        assertEquals(TrampleChange.NONE, Trampling.step(Ground.OTHER, false, 0.1, TrampleSettings.ALWAYS, hit));
    }

    @Test
    void standingStillAndBasesNeverTrample() {
        FixedRandomness hit = FixedRandomness.rolling(0.0);
        assertEquals(TrampleChange.NONE, Trampling.step(Ground.DIRT, false, 0.0, TrampleSettings.ALWAYS, hit));
        assertEquals(TrampleChange.NONE, Trampling.step(Ground.DIRT, true, 0.3, TrampleSettings.ALWAYS, hit));
    }

    @Test
    void gallopsTrampleMoreThanWalks() {
        int walk = trampled(0.1);
        int gallop = trampled(0.35);
        assertTrue(gallop > walk * 3, "walk " + walk + ", gallop " + gallop);
        // Capped at 4x however fast.
        assertTrue(trampled(5.0) < walk * 5, "capped");
    }

    private static int trampled(double speed) {
        FixedRandomness random = FixedRandomness.seeded(7);
        int count = 0;
        for (int i = 0; i < 100_000; i++) {
            if (Trampling.step(Ground.DIRT, false, speed, TrampleSettings.DEFAULT, random) != TrampleChange.NONE) {
                count++;
            }
        }
        return count;
    }

    @Test
    void settingsAreValidated() {
        assertThrows(IllegalArgumentException.class,
                () -> new TrampleSettings(Chance.NEVER, Chance.NEVER, Chance.NEVER, 0.0, 4.0));
        assertThrows(IllegalArgumentException.class,
                () -> new TrampleSettings(Chance.NEVER, Chance.NEVER, Chance.NEVER, 0.1, 0.5));
    }
}
