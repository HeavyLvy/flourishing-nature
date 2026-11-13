package io.github.heavylvy.flourishingnature.core.herd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Starting species profiles from the design doc. All numbers are first guesses for tuning; a
 * datapack loader will override them later.
 *
 * <p>Goats are left out on purpose: they use vanilla's Brain AI rather than goals, so they need a
 * separate approach.
 */
public final class BuiltInProfiles {
    private BuiltInProfiles() {}

    public static final SpeciesId COW = SpeciesId.minecraft("cow");
    public static final SpeciesId MOOSHROOM = SpeciesId.minecraft("mooshroom");
    public static final SpeciesId SHEEP = SpeciesId.minecraft("sheep");
    public static final SpeciesId PIG = SpeciesId.minecraft("pig");
    public static final SpeciesId CHICKEN = SpeciesId.minecraft("chicken");
    public static final SpeciesId HORSE = SpeciesId.minecraft("horse");
    public static final SpeciesId DONKEY = SpeciesId.minecraft("donkey");
    public static final SpeciesId GOAT = SpeciesId.minecraft("goat");

    /** Spread of every species' bell curve, from the design doc's example profile. */
    private static final double SIGMA = 1.5;

    private static final Map<SpeciesId, SpeciesProfile> PROFILES = Map.of(
            COW, profile(COW, 5, 3, 8, 0.08, List.of(IntRange.of(1, 2), IntRange.of(10, 14)),
                    Map.of(MOOSHROOM, 0.9, SHEEP, 0.4, HORSE, 0.3)),
            MOOSHROOM, profile(MOOSHROOM, 5, 3, 8, 0.08, List.of(IntRange.of(1, 2), IntRange.of(10, 14)),
                    Map.of(COW, 0.9, SHEEP, 0.3)),
            SHEEP, profile(SHEEP, 7, 4, 10, 0.05, List.of(IntRange.of(1, 2), IntRange.of(12, 16)),
                    Map.of(COW, 0.4, GOAT, 0.3)),
            PIG, profile(PIG, 3, 2, 5, 0.10, List.of(IntRange.exactly(1), IntRange.of(6, 8)),
                    Map.of(CHICKEN, 0.2)),
            CHICKEN, profile(CHICKEN, 5, 3, 8, 0.05, List.of(IntRange.of(1, 2), IntRange.of(10, 12)),
                    Map.of(PIG, 0.2)),
            HORSE, profile(HORSE, 4, 2, 6, 0.10, List.of(IntRange.exactly(1), IntRange.of(8, 10)),
                    Map.of(DONKEY, 0.6, COW, 0.3)),
            DONKEY, profile(DONKEY, 3, 2, 5, 0.10, List.of(IntRange.exactly(1), IntRange.of(6, 8)),
                    Map.of(HORSE, 0.6)));

    public static Optional<SpeciesProfile> forSpecies(SpeciesId species) {
        return Optional.ofNullable(PROFILES.get(species));
    }

    public static Map<SpeciesId, SpeciesProfile> all() {
        return PROFILES;
    }

    private static SpeciesProfile profile(
            SpeciesId species, double mean, int min, int max, double outlierChance,
            List<IntRange> outliers, Map<SpeciesId, Double> likes) {
        Map<SpeciesId, Affinity> affinities = new HashMap<>();
        likes.forEach((other, weight) -> affinities.put(other, new Affinity(weight)));
        return new SpeciesProfile(
                species,
                new PreferenceDistribution(mean, SIGMA, IntRange.of(min, max), outlierChance, outliers),
                affinities);
    }
}
