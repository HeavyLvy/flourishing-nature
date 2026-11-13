package io.github.heavylvy.flourishingnature.core.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

final class MilkChargesTest {
    private static final MilkSettings SETTINGS = MilkSettings.DEFAULT;

    @Test
    void startsFullAndRefusesWhenEmpty() {
        MilkCharges full = MilkCharges.full(SETTINGS, 0);
        MilkCharges one = full.take(10, 1.0, SETTINGS).orElseThrow();
        MilkCharges none = one.take(20, 1.0, SETTINGS).orElseThrow();
        assertEquals(0, none.charges());
        assertTrue(none.take(30, 1.0, SETTINGS).isEmpty());
    }

    @Test
    void refillsOneChargePerDayAtRateOne() {
        MilkCharges empty = new MilkCharges(0, 0.0, 0);
        assertEquals(0, empty.now(23_999, 1.0, SETTINGS).charges());
        assertEquals(1, empty.now(24_000, 1.0, SETTINGS).charges());
        assertEquals(2, empty.now(48_000, 1.0, SETTINGS).charges());
        assertEquals(2, empty.now(500_000, 1.0, SETTINGS).charges(), "never above the max");
        assertEquals(0.0, empty.now(500_000, 1.0, SETTINGS).progress());
    }

    @Test
    void rateScalesRefillTime() {
        MilkCharges empty = new MilkCharges(0, 0.0, 0);
        assertEquals(1, empty.now(12_000, 2.0, SETTINGS).charges(), "thriving: half a day");
        assertEquals(0, empty.now(71_999, 1.0 / 3.0, SETTINGS).charges(), "stressed: three days");
        assertEquals(1, empty.now(72_001, 1.0 / 3.0, SETTINGS).charges());
        assertEquals(0, empty.now(10_000_000, 0.0, SETTINGS).charges(), "rate 0 never refills");
    }

    @Test
    void keepsPartialProgressAcrossReadings() {
        MilkCharges half = new MilkCharges(0, 0.0, 0).now(12_000, 1.0, SETTINGS);
        assertEquals(0.5, half.progress(), 1.0e-9);
        assertEquals(1, half.now(24_000, 1.0, SETTINGS).charges());
        Optional<MilkCharges> taken = new MilkCharges(1, 0.25, 0).take(0, 1.0, SETTINGS);
        assertEquals(0.25, taken.orElseThrow().progress(), 1.0e-9);
    }

    @Test
    void rejectsBadValues() {
        assertThrows(IllegalArgumentException.class, () -> new MilkCharges(-1, 0.0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MilkCharges(0, 1.0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MilkSettings(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new MilkCharges(0, 0.0, 0).now(1, -1.0, SETTINGS));
    }
}
