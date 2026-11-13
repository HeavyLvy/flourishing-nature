package io.github.heavylvy.flourishingnature.core.herd;

import io.github.heavylvy.flourishingnature.core.random.Randomness;
import java.util.ArrayList;
import java.util.List;

/** Rolls herd preferences for new mobs and babies. See {@link PreferenceDistribution}. */
public final class PreferenceRoller {
    private PreferenceRoller() {}

    /** A fresh preference for a mob that spawned in the world. */
    public static HerdPreference roll(PreferenceDistribution d, Randomness random) {
        double r = random.nextDouble();
        if (r < PreferenceDistribution.CENTRE_CHANCE) {
            return new HerdPreference(d.mean());
        }
        r -= PreferenceDistribution.CENTRE_CHANCE;
        if (r < PreferenceDistribution.NEAR_CHANCE) {
            int step = random.nextDouble() < 0.5 ? -1 : 1;
            int near = d.mean() + step;
            if (near < d.typical().min() || near > d.typical().max()) {
                near = d.mean() - step;
            }
            return HerdPreference.clamped(Math.max(d.typical().min(), Math.min(d.typical().max(), near)));
        }
        r -= PreferenceDistribution.NEAR_CHANCE;
        if (r < d.outlierChance()) {
            IntRange range = d.outlierRanges().get(random.nextIntInclusive(0, d.outlierRanges().size() - 1));
            return new HerdPreference(random.nextIntInclusive(range.min(), range.max()));
        }
        List<Integer> wide = new ArrayList<>();
        for (int v = d.typical().min(); v <= d.typical().max(); v++) {
            if (Math.abs(v - d.mean()) > 1) {
                wide.add(v);
            }
        }
        if (wide.isEmpty()) {
            return new HerdPreference(d.mean());
        }
        return new HerdPreference(wide.get(random.nextIntInclusive(0, wide.size() - 1)));
    }

    /**
     * A baby's preference: rolled like any other, but centred on the average of its parents
     * (rounded), so herds pass their personality on.
     */
    public static HerdPreference inherit(
            PreferenceDistribution distribution, HerdPreference parentA, HerdPreference parentB, Randomness random) {
        int parentMean = (int) Math.round((parentA.value() + parentB.value()) / 2.0);
        return roll(distribution.withMean(parentMean), random);
    }
}
