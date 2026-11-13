package io.github.heavylvy.flourishingnature.core.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.heavylvy.flourishingnature.core.fullness.HungerBand;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;
import org.junit.jupiter.api.Test;

final class ProductionRulesTest {
    @Test
    void starvingAnimalsGiveNothingButNeverDie() {
        assertEquals(0.0, ProductionRules.productRate(Mood.THRIVING, HungerBand.STARVING));
        assertTrue(ProductionRules.minimumDropsOnly(Mood.CONTENT, HungerBand.STARVING));
        assertFalse(ProductionRules.canBreed(Mood.THRIVING, HungerBand.STARVING));
        assertEquals(1, ProductionRules.maxWool(HungerBand.STARVING, 3));
    }

    @Test
    void happyFullAnimalsGiveDoubleAndSelfBreed() {
        assertEquals(2.0, ProductionRules.productRate(Mood.THRIVING, HungerBand.FULL));
        assertEquals(1, ProductionRules.dropRollBonus(Mood.THRIVING, HungerBand.FULL));
        assertTrue(ProductionRules.canSelfBreed(Mood.CONTENT, HungerBand.FULL));
        assertFalse(ProductionRules.canSelfBreed(Mood.CONTENT, HungerBand.FED));
    }

    @Test
    void hungerHalvesProducts() {
        assertEquals(0.5, ProductionRules.productRate(Mood.CONTENT, HungerBand.HUNGRY));
        assertEquals(-1, ProductionRules.dropRollBonus(Mood.CONTENT, HungerBand.HUNGRY));
    }
}
