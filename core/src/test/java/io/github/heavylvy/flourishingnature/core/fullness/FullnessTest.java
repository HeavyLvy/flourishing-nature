package io.github.heavylvy.flourishingnature.core.fullness;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class FullnessTest {
    @Test
    void drainsLazilyOverTime() {
        FullnessMeter meter = new FullnessMeter(Fullness.FULL, 0);
        assertEquals(0.5, meter.now(FullnessRates.DAY, FullnessRates.GRAZER_DRAIN).value(), 1.0e-9);
        assertEquals(0.0, meter.now(10 * FullnessRates.DAY, FullnessRates.GRAZER_DRAIN).value());
    }

    @Test
    void eatingAddsOnTopOfTheCurrentValue() {
        FullnessMeter meter = new FullnessMeter(new Fullness(0.5), 0).eat(0.3, FullnessRates.DAY / 2, FullnessRates.GRAZER_DRAIN);
        assertEquals(0.55, meter.fullness().value(), 1.0e-9);
    }

    @Test
    void bands() {
        assertEquals(HungerBand.FULL, new Fullness(0.9).band());
        assertEquals(HungerBand.FED, new Fullness(0.5).band());
        assertEquals(HungerBand.HUNGRY, new Fullness(0.2).band());
        assertEquals(HungerBand.STARVING, new Fullness(0.1).band());
    }

    @Test
    void aKillKeepsAWolfFedForAboutTwoDays() {
        FullnessMeter fed = FullnessMeter.at(Fullness.FULL, 0);
        assertEquals(HungerBand.FED, fed.now(2 * FullnessRates.DAY - 100, FullnessRates.PREDATOR_DRAIN).band());
        assertEquals(HungerBand.HUNGRY, fed.now(2 * FullnessRates.DAY + 100, FullnessRates.PREDATOR_DRAIN).band());
    }
}
