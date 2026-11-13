package io.github.heavylvy.flourishingnature.core.herd;

import java.util.List;

/**
 * How a species rolls its herd preference. Sharply peaked: most animals want exactly the species
 * average, a few want one more or one less, and the rest spread out, like a class where most kids
 * are the same height and only a handful stand out.
 *
 * <ul>
 *   <li>{@link #CENTRE_CHANCE} (80%): exactly the mean.
 *   <li>{@link #NEAR_CHANCE} (10%): one above or one below.
 *   <li>{@code outlierChance}: a true loner or party animal from one of the outlier ranges.
 *   <li>The rest: anywhere else in the typical range.
 * </ul>
 *
 * @param mean the species average
 * @param typical the range ordinary animals fall in; contains the mean
 * @param outlierChance chance of rolling from the outlier ranges
 * @param outlierRanges ranges an outlier is picked from; empty means no outliers
 */
public record PreferenceDistribution(int mean, IntRange typical, double outlierChance, List<IntRange> outlierRanges) {
    public static final double CENTRE_CHANCE = 0.80;
    public static final double NEAR_CHANCE = 0.10;

    public PreferenceDistribution {
        if (mean < typical.min() || mean > typical.max()) {
            throw new IllegalArgumentException("mean " + mean + " outside typical range " + typical);
        }
        if (!(outlierChance >= 0.0 && outlierChance <= 1.0 - CENTRE_CHANCE - NEAR_CHANCE)) {
            throw new IllegalArgumentException("outlier chance must be in [0, 0.1]: " + outlierChance);
        }
        if (outlierChance > 0.0 && outlierRanges.isEmpty()) {
            throw new IllegalArgumentException("outlier chance set but no outlier ranges given");
        }
        outlierRanges = List.copyOf(outlierRanges);
    }

    /** Same distribution recentred on a new mean (clamped to the typical range), for babies. */
    public PreferenceDistribution withMean(int newMean) {
        int clamped = Math.max(typical.min(), Math.min(typical.max(), newMean));
        return new PreferenceDistribution(clamped, typical, outlierChance, outlierRanges);
    }
}
