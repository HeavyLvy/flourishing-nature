package io.github.heavylvy.flourishingnature.core.wellbeing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class WellbeingTest {
    private static final WellbeingProfile COW = WellbeingProfile.grazer();
    private static final WellbeingSettings SETTINGS = WellbeingSettings.DEFAULT;

    /** A sunny meadow with plenty of grass and the herd it wants. */
    private static Surroundings meadow() {
        return new Surroundings(BiomeFit.PREFERRED, true, false, false, 30, 100_000L, 2, 4.0, new HerdPreference(5));
    }

    /** A dark, crowded, bare shed in a biome it dislikes, never fed. */
    private static Surroundings shed() {
        return new Surroundings(BiomeFit.DISLIKED, false, false, true, 0, 100_000L, 10, 10.0, new HerdPreference(3));
    }

    @Test
    void meadowCowThrives() {
        WellbeingState state = WellbeingState.first(meadow(), COW, SETTINGS);
        assertEquals(100.0, state.wellbeing().value(), 1.0e-9);
        assertEquals(Mood.THRIVING, state.wellbeing().mood());
    }

    @Test
    void shedCowIsMiserable() {
        WellbeingState state = WellbeingState.first(shed(), COW, SETTINGS);
        assertTrue(state.wellbeing().value() < 25.0, "shed cow at " + state.wellbeing().value());
        assertEquals(Mood.MISERABLE, state.wellbeing().mood());
    }

    @Test
    void wellbeingDriftsSlowly() {
        WellbeingState state = WellbeingState.first(shed(), COW, SETTINGS);
        double before = state.wellbeing().value();
        state = state.next(meadow(), COW, SETTINGS);
        assertEquals(before + SETTINGS.maxStepPerCheck(), state.wellbeing().value(), 1.0e-9);
    }

    @Test
    void movingToAGoodPenPaysOffWithinAnHourOfPlay() {
        WellbeingState state = WellbeingState.first(shed(), COW, SETTINGS);
        int checks = 72_000 / SETTINGS.checkInterval();
        for (int i = 0; i < checks; i++) {
            state = state.next(meadow(), COW, SETTINGS);
        }
        assertEquals(Mood.THRIVING, state.wellbeing().mood());
    }

    @Test
    void duckingUnderATreeBrieflyBarelyHurtsSky() {
        WellbeingState state = WellbeingState.first(meadow(), COW, SETTINGS);
        Surroundings underTree = new Surroundings(BiomeFit.PREFERRED, false, false, true, 30, 100_000L, 2, 4.0, new HerdPreference(5));
        state = state.next(underTree, COW, SETTINGS);
        assertTrue(state.needs().of(Need.SKY).value() > 0.8, "one moment of shade dropped sky to " + state.needs().of(Need.SKY));
    }

    @Test
    void rainOnlyMattersWithoutARoof() {
        Surroundings rainOpenField = new Surroundings(BiomeFit.PREFERRED, true, true, false, 30, 100_000L, 2, 4.0, new HerdPreference(5));
        Surroundings rainNearBarn = new Surroundings(BiomeFit.PREFERRED, true, true, true, 30, 100_000L, 2, 4.0, new HerdPreference(5));
        assertEquals(SETTINGS.exposedShelterScore(),
                WellbeingState.first(rainOpenField, COW, SETTINGS).needs().of(Need.SHELTER).value(), 1.0e-9);
        assertEquals(1.0, WellbeingState.first(rainNearBarn, COW, SETTINGS).needs().of(Need.SHELTER).value(), 1.0e-9);
    }

    @Test
    void spaceIsFineUpToTheComfortLimitThenFallsFast() {
        assertEquals(1.0, NeedRules.space(4, 4, 24).value());
        assertEquals(0.5, NeedRules.space(6, 4, 24).value(), 1.0e-9);
        assertEquals(0.0, NeedRules.space(8, 4, 24).value());
        assertEquals(0.0, NeedRules.space(23, 10, 24).value());
    }

    @Test
    void socialPeaksNearPreference() {
        assertEquals(1.0, NeedRules.social(0.0, 1).value(), "loner alone");
        assertEquals(1.0, NeedRules.social(3.5, 5).value(), "within one of 5");
        assertTrue(NeedRules.social(0.0, 8).value() < 0.5, "social cow alone");
        assertTrue(NeedRules.social(12.0, 2).value() < 0.5, "quiet cow in a crowd");
    }

    @Test
    void handFeedingFillsFoodOnBarePens() {
        Surroundings fedToday = new Surroundings(BiomeFit.PREFERRED, true, false, false, 0, 1_000L, 2, 4.0, new HerdPreference(5));
        assertTrue(NeedRules.food(fedToday, COW, SETTINGS).value() > 0.9);
    }

    @Test
    void valueTypesRejectNonsense() {
        assertThrows(IllegalArgumentException.class, () -> new Wellbeing(101));
        assertThrows(IllegalArgumentException.class, () -> new NeedScore(-0.1));
        assertThrows(IllegalArgumentException.class, () -> new NeedWeights(Map.of(Need.FOOD, 1.0)));
    }
}
