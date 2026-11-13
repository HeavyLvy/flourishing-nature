package io.github.heavylvy.flourishingnature.core.herbivore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SelfBreedingTest {
    private static final SelfBreedingSettings S = SelfBreedingSettings.DEFAULT;
    private static final long NEVER = Long.MIN_VALUE / 2;

    private static SelfBreeding.Verdict check(boolean canLove, Mood mood, HungerBand hunger, long now, long last, int nearby) {
        return SelfBreeding.check(canLove, mood, hunger, now, last, nearby, 12, S);
    }

    @Test
    void fullHappyAdultsBreed() {
        assertEquals(SelfBreeding.Verdict.READY, check(true, Mood.CONTENT, HungerBand.FULL, 100, NEVER, 2));
        assertEquals(SelfBreeding.Verdict.READY, check(true, Mood.THRIVING, HungerBand.FULL, 100, NEVER, 2));
    }

    @Test
    void hungryOrStressedAnimalsDoNot() {
        assertEquals(SelfBreeding.Verdict.NOT_WELL_ENOUGH, check(true, Mood.CONTENT, HungerBand.FED, 100, NEVER, 2));
        assertEquals(SelfBreeding.Verdict.NOT_WELL_ENOUGH, check(true, Mood.STRESSED, HungerBand.FULL, 100, NEVER, 2));
        assertEquals(SelfBreeding.Verdict.NOT_READY_TO_LOVE, check(false, Mood.THRIVING, HungerBand.FULL, 100, NEVER, 2));
    }

    @Test
    void waitsAFewDaysBetweenLitters() {
        long last = 1_000L;
        assertEquals(SelfBreeding.Verdict.COOLING_DOWN, check(true, Mood.CONTENT, HungerBand.FULL, last + S.cooldownTicks() - 1, last, 2));
        assertEquals(SelfBreeding.Verdict.READY, check(true, Mood.CONTENT, HungerBand.FULL, last + S.cooldownTicks(), last, 2));
    }

    @Test
    void crowdedAreasStopBreeding() {
        assertEquals(SelfBreeding.Verdict.CROWDED, check(true, Mood.CONTENT, HungerBand.FULL, 100, NEVER, 12));
        assertEquals(SelfBreeding.Verdict.READY, check(true, Mood.CONTENT, HungerBand.FULL, 100, NEVER, 11));
    }

    @Test
    void capsParseFromConfigLines() {
        List<String> rejected = new ArrayList<>();
        PopulationCaps caps = PopulationCaps.parse(12, List.of("minecraft:chicken=16", " minecraft:pig = 6 ", "bad", "x:y=0", "=4"), rejected);
        assertEquals(16, caps.capFor(SpeciesId.minecraft("chicken")));
        assertEquals(6, caps.capFor(SpeciesId.minecraft("pig")));
        assertEquals(12, caps.capFor(SpeciesId.minecraft("cow")));
        assertEquals(List.of("bad", "x:y=0", "=4"), rejected);
        assertThrows(IllegalArgumentException.class, () -> new PopulationCaps(0, java.util.Map.of()));
    }
}
