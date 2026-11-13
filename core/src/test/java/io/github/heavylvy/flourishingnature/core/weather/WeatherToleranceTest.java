package io.github.heavylvy.flourishingnature.core.weather;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import org.junit.jupiter.api.Test;

final class WeatherToleranceTest {
    @Test
    void farmAnimalsSeekShelterButWolvesAndFoxesDoNot() {
        assertTrue(WeatherTolerance.seeksShelter(SpeciesId.minecraft("cow")));
        assertTrue(WeatherTolerance.seeksShelter(SpeciesId.minecraft("chicken")));
        assertFalse(WeatherTolerance.seeksShelter(SpeciesId.minecraft("wolf")));
        assertFalse(WeatherTolerance.seeksShelter(SpeciesId.minecraft("fox")));
    }
}
