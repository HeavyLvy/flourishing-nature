package io.github.heavylvy.flourishingnature.core.predator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.fullness.FullnessMeter;
import io.github.heavylvy.flourishingnature.core.fullness.FullnessRates;
import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.random.Randomness;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class PackRulesTest {
    private static final PackSettings S = PackSettings.DEFAULT;
    private static final Position ORIGIN = new Position(0, 64, 0);

    @Test
    void onlyHungryWolvesHuntAndStarvingOnesTakeMore() {
        assertFalse(Diet.of(HungerBand.FULL).hunts());
        assertFalse(Diet.of(HungerBand.FED).hunts());
        assertEquals(Diet.USUAL_PREY, Diet.of(HungerBand.HUNGRY));
        assertEquals(Diet.ANY_PREY, Diet.of(HungerBand.STARVING));
    }

    @Test
    void weakAndYoungPreyComeFirst() {
        Prey healthyClose = new Prey(1, new Position(8, 64, 0), false, 1.0);
        Prey lambFurther = new Prey(2, new Position(20, 64, 0), true, 1.0);
        assertEquals(Optional.of(lambFurther), PreyChoice.pick(ORIGIN, List.of(healthyClose, lambFurther), 48));
        Prey hurtAdult = new Prey(3, new Position(12, 64, 0), false, 0.25);
        assertEquals(Optional.of(hurtAdult), PreyChoice.pick(ORIGIN, List.of(healthyClose, hurtAdult), 48));
    }

    @Test
    void aHealthyAnimalRightThereBeatsAFarBaby() {
        Prey healthyClose = new Prey(1, new Position(3, 64, 0), false, 1.0);
        Prey farLamb = new Prey(2, new Position(40, 64, 0), true, 1.0);
        assertEquals(Optional.of(healthyClose), PreyChoice.pick(ORIGIN, List.of(healthyClose, farLamb), 48));
    }

    @Test
    void preyOutOfRangeIsIgnored() {
        assertEquals(Optional.empty(), PreyChoice.pick(ORIGIN, List.of(new Prey(1, new Position(60, 64, 0), true, 0.1)), 48));
        assertThrows(IllegalArgumentException.class, () -> new Prey(1, ORIGIN, false, 1.5));
    }

    @Test
    void packDriftsTowardPreyAndStopsShort() {
        Position step = PackRules.driftToward(ORIGIN, new Position(40, 64, 0), S);
        assertEquals(S.driftStep(), step.x(), 1.0e-9);
        Position close = PackRules.driftToward(new Position(37, 64, 0), new Position(40, 64, 0), S);
        assertEquals(37.0, close.x(), 1.0e-9);
        Position last = PackRules.driftToward(new Position(32, 64, 0), new Position(40, 64, 0), S);
        assertEquals(40.0 - S.driftStopWithin(), last.x(), 1.0e-9);
    }

    @Test
    void packHungerIsTheAverage() {
        assertEquals(0.5, PackRules.packFullness(List.of(new Fullness(0.2), new Fullness(0.8))).value(), 1.0e-9);
        assertThrows(IllegalArgumentException.class, () -> PackRules.packFullness(List.of()));
    }

    @Test
    void aFullPackWithRoomBreeds() {
        assertTrue(PackRules.mayBreed(2, 4, HungerBand.FULL, S));
        assertFalse(PackRules.mayBreed(2, 4, HungerBand.FED, S));
        assertFalse(PackRules.mayBreed(1, 4, HungerBand.FULL, S));
        assertFalse(PackRules.mayBreed(4, S.maxBreedingPackSize(), HungerBand.FULL, S));
    }

    @Test
    void breedingCooldownLastsAFewDays() {
        PackMemory justBred = PackMemory.fresh(0).bred(1000);
        assertFalse(PackRules.pastBreedCooldown(justBred, 1000 + FullnessRates.DAY, S));
        assertTrue(PackRules.pastBreedCooldown(justBred, 1000 + 3 * FullnessRates.DAY, S));
        assertTrue(PackRules.pastBreedCooldown(PackMemory.fresh(0), 0, S));
    }

    @Test
    void aStarvingPackSplitsOnlyAfterSeveralDays() {
        List<HungerBand> starving = List.of(HungerBand.STARVING, HungerBand.STARVING);
        assertFalse(PackRules.shouldSplit(starving, 0, 3 * FullnessRates.DAY, S));
        assertTrue(PackRules.shouldSplit(starving, 0, 4 * FullnessRates.DAY, S));
        assertFalse(PackRules.shouldSplit(List.of(HungerBand.STARVING, HungerBand.HUNGRY), 0, 10 * FullnessRates.DAY, S));
        assertFalse(PackRules.shouldSplit(List.of(), 0, 10 * FullnessRates.DAY, S));
    }

    @Test
    void aWolfLeftAloneAfterAKillStarvesBeforeThePackSplits() {
        FullnessMeter kill = FullnessMeter.at(Fullness.FULL, 0);
        long split = S.splitAfter();
        assertEquals(HungerBand.STARVING, kill.now(split, FullnessRates.PREDATOR_DRAIN).band());
    }

    @Test
    void brokenPackScattersFarInDifferentDirections() {
        List<Position> targets = PackRules.dispersalTargets(ORIGIN, 4, new Seeded(7), S);
        assertEquals(4, targets.size());
        for (Position t : targets) {
            double d = Math.hypot(t.x(), t.z());
            assertTrue(d >= S.dispersalMin() - 1.0e-9 && d <= S.dispersalMax() + 1.0e-9, "distance " + d);
        }
        for (int i = 0; i < targets.size(); i++) {
            for (int j = i + 1; j < targets.size(); j++) {
                assertTrue(targets.get(i).distanceTo(targets.get(j)) > 40, "two wolves head the same way");
            }
        }
    }

    @Test
    void dispersalEndsOnArrivalOrTimeout() {
        Dispersal d = new Dispersal(new Position(100, 64, 0), 500);
        assertTrue(d.active(0, ORIGIN));
        assertFalse(d.active(0, new Position(97, 70, 0)));
        assertFalse(d.active(500, ORIGIN));
    }

    private record Seeded(Random random) implements Randomness {
        Seeded(long seed) {
            this(new Random(seed));
        }

        @Override
        public double nextDouble() {
            return random.nextDouble();
        }

        @Override
        public double nextGaussian() {
            return random.nextGaussian();
        }

        @Override
        public int nextIntInclusive(int min, int max) {
            return random.nextInt(min, max + 1);
        }
    }
}
