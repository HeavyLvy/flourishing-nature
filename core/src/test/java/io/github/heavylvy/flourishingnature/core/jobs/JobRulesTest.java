package io.github.heavylvy.flourishingnature.core.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.random.Randomness;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class JobRulesTest {
    @Test
    void chancePerCheckAddsUpToTheDailyRate() {
        assertEquals(1.0 / 120, JobChance.perCheck(1.0, 200), 1e-12);
        assertEquals(1.0, JobChance.perCheck(1000, 200));
        assertEquals(0.0, JobChance.perCheck(0, 200));
        assertThrows(IllegalArgumentException.class, () -> JobChance.perCheck(-1, 200));
    }

    @Test
    void randomTickChanceStepsAboutOncePerDay() {
        double p = JobChance.perRandomTick(1.0, 3);
        double randomTicksPerDay = 3 / 4096.0 * JobChance.DAY;
        assertEquals(1.0, p * randomTicksPerDay, 1e-9);
        assertEquals(0.0, JobChance.perRandomTick(1.0, 0));
        assertTrue(JobChance.perRandomTick(3.0, 3) < p);
    }

    @Test
    void patDriesInThreeStepsThenCrumbles() {
        PatAge age = PatAge.FRESH;
        int steps = 0;
        while (age.fresh()) {
            age = age.next().orElseThrow();
            steps++;
        }
        assertEquals(3, steps);
        assertTrue(age.dried());
        assertEquals(Optional.empty(), age.next());
        assertThrows(IllegalArgumentException.class, () -> new PatAge(4));
    }

    @Test
    void onlyFedHappyUncrampedCowsLeavePats() {
        Set<HungerBand> fed = EnumSet.of(HungerBand.FULL, HungerBand.FED);
        Set<Mood> happy = EnumSet.of(Mood.THRIVING, Mood.CONTENT);
        for (HungerBand band : HungerBand.values()) {
            for (Mood mood : Mood.values()) {
                boolean expected = fed.contains(band) && happy.contains(mood);
                assertEquals(expected, CowPatRules.mayLeavePat(band, mood, false), band + " " + mood);
                assertFalse(CowPatRules.mayLeavePat(band, mood, true), "crammed " + band + " " + mood);
            }
        }
    }

    @Test
    void thrivingCowsLeaveTwiceAsMany() {
        double content = CowPatRules.chancePerCheck(HungerBand.FED, Mood.CONTENT, false, 1.0, 200);
        double thriving = CowPatRules.chancePerCheck(HungerBand.FULL, Mood.THRIVING, false, 1.0, 200);
        assertEquals(2 * content, thriving, 1e-12);
        assertEquals(0.0, CowPatRules.chancePerCheck(HungerBand.STARVING, Mood.THRIVING, false, 1.0, 200));
        assertEquals(0.0, CowPatRules.chancePerCheck(HungerBand.FULL, Mood.THRIVING, true, 1.0, 200));
    }

    @Test
    void spotsCoolDownThenOpenAgain() {
        long cooldown = 3 * JobChance.DAY;
        BlockSpot here = new BlockSpot(10, 64, 10);
        SpotMemory memory = SpotMemory.EMPTY.remember(here, 100, cooldown, 4);
        assertFalse(memory.mayWorkAt(here, 200, cooldown, 2));
        assertFalse(memory.mayWorkAt(new BlockSpot(12, 64, 9), 200, cooldown, 2));
        assertTrue(memory.mayWorkAt(new BlockSpot(13, 64, 10), 200, cooldown, 2));
        assertTrue(memory.mayWorkAt(here, 100 + cooldown, cooldown, 2));
    }

    @Test
    void spotMemoryForgetsOldestAndExpired() {
        long cooldown = 1000;
        SpotMemory memory = SpotMemory.EMPTY;
        for (int i = 0; i < 6; i++) {
            memory = memory.remember(new BlockSpot(i * 10, 0, 0), i, cooldown, 4);
        }
        assertEquals(4, memory.spots().size());
        assertTrue(memory.mayWorkAt(new BlockSpot(0, 0, 0), 10, cooldown, 2));
        SpotMemory later = memory.remember(new BlockSpot(500, 0, 0), 5000, cooldown, 4);
        assertEquals(List.of(new WorkedSpot(new BlockSpot(500, 0, 0), 5000)), later.spots());
    }

    @Test
    void rootingFindsAreRareAndVaried() {
        Randomness random = seeded(7);
        Set<RootingFind> seen = EnumSet.noneOf(RootingFind.class);
        int finds = 0;
        for (int i = 0; i < 10_000; i++) {
            RootingFind find = RootingFind.roll(random, 0.2);
            seen.add(find);
            if (find != RootingFind.NOTHING) {
                finds++;
            }
        }
        assertEquals(EnumSet.allOf(RootingFind.class), seen);
        assertEquals(2000, finds, 150);
        assertEquals(RootingFind.NOTHING, RootingFind.roll(random, 0.0));
    }

    @Test
    void horsesPawOnlyAtLowGround() {
        assertTrue(WaterHoleRules.lowGround(64, List.of(64, 64, 65, 66)));
        assertFalse(WaterHoleRules.lowGround(64, List.of(64, 63, 65)));
        assertFalse(WaterHoleRules.lowGround(64, List.of()));
    }

    @Test
    void rainResetsTheDrySpell() {
        int dryness = 0;
        for (int i = 0; i < WaterHoleRules.MAX_DRYNESS; i++) {
            assertFalse(WaterHoleRules.driedUp(dryness));
            dryness = WaterHoleRules.nextDryness(dryness, false, true);
        }
        assertTrue(WaterHoleRules.driedUp(dryness));
        assertEquals(0, WaterHoleRules.nextDryness(5, true, true));
        assertEquals(5, WaterHoleRules.nextDryness(5, false, false));
    }

    @Test
    void seedsNeedFleeceAndGrass() {
        assertTrue(SeedRules.mayCatch(false, false));
        assertFalse(SeedRules.mayCatch(true, false));
        assertFalse(SeedRules.mayCatch(false, true));
        assertFalse(SeedRules.readyToDrop(100, 150, 100));
        assertTrue(SeedRules.readyToDrop(100, 200, 100));
        assertTrue(SeedRules.takesRoot(true, true));
        assertFalse(SeedRules.takesRoot(false, true));
        assertFalse(SeedRules.takesRoot(true, false));
    }

    private static Randomness seeded(long seed) {
        Random r = new Random(seed);
        return new Randomness() {
            @Override
            public double nextDouble() {
                return r.nextDouble();
            }

            @Override
            public double nextGaussian() {
                return r.nextGaussian();
            }

            @Override
            public int nextIntInclusive(int min, int max) {
                return r.nextInt(min, max + 1);
            }
        };
    }
}
