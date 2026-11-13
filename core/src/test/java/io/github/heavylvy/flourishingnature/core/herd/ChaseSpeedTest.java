package io.github.heavylvy.flourishingnature.core.herd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ChaseSpeedTest {
    @Test
    void sheepRunJustSlowerThanAWolf() {
        double wolfChase = 0.3;
        double sheepBase = 0.23;
        double flee = ChaseSpeed.fleeModifier(sheepBase, wolfChase, 1.8);
        assertTrue(sheepBase * flee < wolfChase, "sheep outruns the wolf");
        assertEquals(0.9 * wolfChase, sheepBase * flee, 1.0e-9);
    }

    @Test
    void withoutAHunterPanicSpeedIsUnchanged() {
        assertEquals(1.8, ChaseSpeed.fleeModifier(0.2, Double.POSITIVE_INFINITY, 1.8));
    }

    @Test
    void neverFasterThanNormalPanic() {
        assertEquals(1.25, ChaseSpeed.fleeModifier(0.1, 1.0, 1.25));
    }
}
