package io.github.heavylvy.flourishingnature.core.nest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.random.Randomness;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class NestTest {
    private static final NestSettings SETTINGS = NestSettings.DEFAULT;

    private static Clutch clutch(NestEgg... eggs) {
        return new Clutch(List.of(eggs));
    }

    @Test
    void aNestHoldsFourEggs() {
        Clutch full = clutch(new NestEgg.Plain(), new NestEgg.Plain(), new NestEgg.Plain(), new NestEgg.Plain());
        assertTrue(full.isFull());
        assertTrue(full.with(new NestEgg.Plain()).isEmpty());
        assertEquals(4, clutch(new NestEgg.Plain(), new NestEgg.Plain(), new NestEgg.Plain()).with(new NestEgg.Plain()).orElseThrow().size());
        assertThrows(IllegalArgumentException.class,
                () -> clutch(new NestEgg.Plain(), new NestEgg.Plain(), new NestEgg.Plain(), new NestEgg.Plain(), new NestEgg.Plain()));
    }

    @Test
    void playersTakeTheNewestEgg() {
        Clutch.Taken taken = clutch(new NestEgg.Plain(), NestEgg.Fertile.fresh()).takeNewest().orElseThrow();
        assertEquals(NestEgg.Fertile.fresh(), taken.egg());
        assertEquals(clutch(new NestEgg.Plain()), taken.rest());
        assertTrue(Clutch.EMPTY.takeNewest().isEmpty());
    }

    @Test
    void warmthAddsUpAcrossBoutsAndHatchesAtTheThreshold() {
        Clutch eggs = clutch(new NestEgg.Plain(), NestEgg.Fertile.fresh());
        Clutch.Incubation first = eggs.incubate(6000, 12000);
        assertEquals(0, first.hatched());
        assertEquals(clutch(new NestEgg.Plain(), new NestEgg.Fertile(6000)), first.rest());
        // A pause (no incubate call) changes nothing; the next bout picks up where it left off.
        Clutch.Incubation second = first.rest().incubate(5999, 12000);
        assertEquals(0, second.hatched());
        Clutch.Incubation third = second.rest().incubate(1, 12000);
        assertEquals(1, third.hatched());
        assertEquals(clutch(new NestEgg.Plain()), third.rest());
    }

    @Test
    void plainEggsNeverHatch() {
        assertEquals(0, clutch(new NestEgg.Plain()).incubate(1_000_000, 12000).hatched());
        assertFalse(clutch(new NestEgg.Plain()).hasFertile());
    }

    @Test
    void layingFollowsProductionRules() {
        LayProgress content = LayProgress.NONE.advance(9000, Mood.CONTENT, HungerBand.FED, SETTINGS);
        assertTrue(content.ready(SETTINGS));
        LayProgress hungry = LayProgress.NONE.advance(9000, Mood.CONTENT, HungerBand.HUNGRY, SETTINGS);
        assertFalse(hungry.ready(SETTINGS));
        assertTrue(hungry.advance(9000, Mood.CONTENT, HungerBand.HUNGRY, SETTINGS).ready(SETTINGS));
        assertTrue(LayProgress.NONE.advance(4500, Mood.THRIVING, HungerBand.FULL, SETTINGS).ready(SETTINGS));
        assertEquals(0.0, LayProgress.NONE.advance(1_000_000, Mood.CONTENT, HungerBand.STARVING, SETTINGS).ticks());
        assertEquals(0.0, LayProgress.NONE.advance(1_000_000, Mood.MISERABLE, HungerBand.FULL, SETTINGS).ticks());
    }

    @Test
    void progressDoesNotBankMoreThanOneEgg() {
        LayProgress waited = LayProgress.NONE.advance(1_000_000, Mood.THRIVING, HungerBand.FULL, SETTINGS);
        assertEquals((double) SETTINGS.layIntervalTicks(), waited.ticks());
    }

    @Test
    void eightPointsBuildANest() {
        assertFalse(BuildPoints.NONE.plus(7).enoughFor(SETTINGS));
        assertTrue(BuildPoints.NONE.plus(2).plus(2).plus(2).plus(1).plus(1).enoughFor(SETTINGS));
        assertThrows(IllegalArgumentException.class, () -> new BuildPoints(-1));
    }

    @Test
    void onlyAbandonedWildNestsDecay() {
        long days = SETTINGS.wildDecayTicks();
        assertTrue(NestRules.decays(false, 0, days, SETTINGS));
        assertFalse(NestRules.decays(false, 0, days - 1, SETTINGS));
        assertFalse(NestRules.decays(true, 0, days * 100, SETTINGS));
    }

    @Test
    void ownership() {
        UUID hen = UUID.randomUUID();
        assertTrue(new NestOwner.Hen(hen).isOwnedBy(hen));
        assertFalse(new NestOwner.Hen(hen).isOwnedBy(UUID.randomUUID()));
        assertFalse(new NestOwner.Unowned().isOwnedBy(hen));
    }

    @Test
    void hensBroodOnlyFertileEggs() {
        Randomness low = fixed(0.1);
        Randomness high = fixed(0.9);
        assertTrue(NestRules.startsBrooding(true, low, SETTINGS));
        assertFalse(NestRules.startsBrooding(true, high, SETTINGS));
        assertFalse(NestRules.startsBrooding(false, low, SETTINGS));
    }

    @Test
    void nestSitesPreferCoverThenHome() {
        Position home = new Position(0, 64, 0);
        NestRules.Site nearOpen = new NestRules.Site(new Position(1, 64, 0), false);
        NestRules.Site farCovered = new NestRules.Site(new Position(5, 64, 0), true);
        NestRules.Site nearCovered = new NestRules.Site(new Position(3, 64, 0), true);
        assertEquals(Optional.of(nearCovered), NestRules.bestSite(List.of(nearOpen, farCovered, nearCovered), home));
        assertEquals(Optional.of(nearOpen), NestRules.bestSite(List.of(nearOpen), home));
        assertTrue(NestRules.bestSite(List.of(), home).isEmpty());
    }

    @Test
    void settingsAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> new NestSettings(16, 12, 8, 12000, 72000, 9000, 1.5, 1200));
        assertThrows(IllegalArgumentException.class, () -> new NestSettings(0, 12, 8, 12000, 72000, 9000, 0.5, 1200));
    }

    private static Randomness fixed(double value) {
        return new Randomness() {
            @Override
            public double nextDouble() {
                return value;
            }

            @Override
            public double nextGaussian() {
                return 0;
            }

            @Override
            public int nextIntInclusive(int min, int max) {
                return min;
            }
        };
    }
}
