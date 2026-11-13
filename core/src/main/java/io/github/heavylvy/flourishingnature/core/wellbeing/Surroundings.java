package io.github.heavylvy.flourishingnature.core.wellbeing;

import io.github.heavylvy.flourishingnature.core.fullness.Fullness;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;

/**
 * What an animal noticed about its surroundings in one check. The loader module fills this in
 * from the world; core turns it into need scores.
 *
 * @param biome how it likes the biome here
 * @param skyNearby it can see the sky here or within a few blocks
 * @param precipitating rain or snow is falling at its position's biome right now
 * @param roofNearby a roof is within reach, at its position or nearby
 * @param fullness how fed it is right now
 * @param eatsPlants whether it lives off plants at all; for one that doesn't (wolves), food is
 *     fullness alone
 * @param ediblePlantsNearby plants it eats growing within its food radius
 * @param ticksSinceMeal ticks since it was last hand-fed or grazed
 * @param mobsCloseBy other mobs within the close-quarters radius
 * @param socialCount herd mates it feels it has (see the herding social scan)
 * @param preference its herd preference
 */
public record Surroundings(
        BiomeFit biome,
        boolean skyNearby,
        boolean precipitating,
        boolean roofNearby,
        Fullness fullness,
        boolean eatsPlants,
        int ediblePlantsNearby,
        long ticksSinceMeal,
        int mobsCloseBy,
        double socialCount,
        HerdPreference preference) {

    public Surroundings {
        if (ediblePlantsNearby < 0 || mobsCloseBy < 0 || ticksSinceMeal < 0 || !(socialCount >= 0.0)) {
            throw new IllegalArgumentException("surroundings counts must be 0 or more");
        }
    }
}
