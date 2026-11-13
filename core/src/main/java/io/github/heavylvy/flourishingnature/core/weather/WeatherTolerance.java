package io.github.heavylvy.flourishingnature.core.weather;

import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import java.util.Set;

/**
 * Which animals head for cover when it rains or snows. Farm animals do, like cows bunching under a
 * tree in a summer shower. Wild hunters with thick coats (wolves, foxes) carry on as usual.
 */
public final class WeatherTolerance {
    private WeatherTolerance() {}

    private static final Set<SpeciesId> SEEK_SHELTER = Set.of(
            SpeciesId.minecraft("cow"),
            SpeciesId.minecraft("mooshroom"),
            SpeciesId.minecraft("sheep"),
            SpeciesId.minecraft("pig"),
            SpeciesId.minecraft("chicken"),
            SpeciesId.minecraft("horse"),
            SpeciesId.minecraft("donkey"),
            SpeciesId.minecraft("rabbit"));

    public static boolean seeksShelter(SpeciesId species) {
        return SEEK_SHELTER.contains(species);
    }
}
