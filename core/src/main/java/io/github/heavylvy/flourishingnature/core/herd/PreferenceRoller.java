package io.github.heavylvy.flourishingnature.core.herd;

import io.github.heavylvy.flourishingnature.core.random.Randomness;

/** Rolls herd preferences for new mobs and babies. */
public final class PreferenceRoller {
    private PreferenceRoller() {}

    /** A fresh preference for a mob that spawned in the world. */
    public static HerdPreference roll(PreferenceDistribution distribution, Randomness random) {
        if (random.nextDouble() < distribution.outlierChance()) {
            IntRange range = distribution.outlierRanges()
                    .get(random.nextIntInclusive(0, distribution.outlierRanges().size() - 1));
            return new HerdPreference(random.nextIntInclusive(range.min(), range.max()));
        }
        long rolled = Math.round(distribution.mean() + distribution.sigma() * random.nextGaussian());
        long clamped = Math.max(distribution.typical().min(), Math.min(distribution.typical().max(), rolled));
        return HerdPreference.clamped(clamped);
    }

    /**
     * A baby's preference: rolled like any other, but centred on the average of its parents, so
     * herds pass their personality on.
     */
    public static HerdPreference inherit(
            PreferenceDistribution distribution, HerdPreference parentA, HerdPreference parentB, Randomness random) {
        double parentMean = (parentA.value() + parentB.value()) / 2.0;
        return roll(distribution.withMean(parentMean), random);
    }
}
