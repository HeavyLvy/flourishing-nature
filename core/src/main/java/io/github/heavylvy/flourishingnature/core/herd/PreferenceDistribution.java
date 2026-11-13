package io.github.heavylvy.flourishingnature.core.herd;

import java.util.List;

/**
 * How a species rolls its herd preference: a bell curve clamped to a typical range, plus a small
 * chance of an outlier (a true loner or a party animal) picked uniformly from one of the outlier
 * ranges.
 *
 * @param mean centre of the bell curve
 * @param sigma spread of the bell curve, above 0
 * @param typical the range normal rolls are clamped to
 * @param outlierChance chance in [0, 1] of rolling an outlier instead
 * @param outlierRanges ranges an outlier is picked from; empty means no outliers
 */
public record PreferenceDistribution(
        double mean, double sigma, IntRange typical, double outlierChance, List<IntRange> outlierRanges) {

    public PreferenceDistribution {
        if (!(sigma > 0.0) || !Double.isFinite(sigma)) {
            throw new IllegalArgumentException("sigma must be a positive number: " + sigma);
        }
        if (!(mean >= typical.min() && mean <= typical.max())) {
            throw new IllegalArgumentException("mean " + mean + " outside typical range " + typical);
        }
        if (!(outlierChance >= 0.0 && outlierChance <= 1.0)) {
            throw new IllegalArgumentException("outlier chance must be in [0, 1]: " + outlierChance);
        }
        if (outlierChance > 0.0 && outlierRanges.isEmpty()) {
            throw new IllegalArgumentException("outlier chance set but no outlier ranges given");
        }
        outlierRanges = List.copyOf(outlierRanges);
    }

    /** Same distribution recentred on a new mean, for inherited preferences. */
    public PreferenceDistribution withMean(double newMean) {
        double clampedMean = Math.max(typical.min(), Math.min(typical.max(), newMean));
        return new PreferenceDistribution(clampedMean, sigma, typical, outlierChance, outlierRanges);
    }
}
