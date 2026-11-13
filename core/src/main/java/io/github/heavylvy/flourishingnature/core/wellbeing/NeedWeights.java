package io.github.heavylvy.flourishingnature.core.wellbeing;

import java.util.EnumMap;
import java.util.Map;

/**
 * How much each need matters to a species. Weights are relative: only their ratios matter.
 *
 * @param weights a weight of 0 or more for every {@link Need}, with a positive total
 */
public record NeedWeights(Map<Need, Double> weights) {
    public NeedWeights {
        double total = 0.0;
        for (Need need : Need.values()) {
            double weight = weights.getOrDefault(need, -1.0);
            if (!(weight >= 0.0) || !Double.isFinite(weight)) {
                throw new IllegalArgumentException("need weight for " + need + " must be a number 0 or more");
            }
            total += weight;
        }
        if (!(total > 0.0)) {
            throw new IllegalArgumentException("need weights must not all be 0");
        }
        weights = Map.copyOf(new EnumMap<>(weights));
    }

    public double of(Need need) {
        return weights.getOrDefault(need, 0.0);
    }

    /** The design doc's cow weights: biome 15, sky 15, shelter 10, food 25, space 20, social 15. */
    public static NeedWeights grazer() {
        return new NeedWeights(Map.of(
                Need.BIOME, 15.0, Need.SKY, 15.0, Need.SHELTER, 10.0,
                Need.FOOD, 25.0, Need.SPACE, 20.0, Need.SOCIAL, 15.0));
    }
}
