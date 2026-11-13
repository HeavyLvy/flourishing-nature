package io.github.heavylvy.flourishingnature.core.herd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class PreferenceRollerTest {
    private static final PreferenceDistribution COW =
            BuiltInProfiles.forSpecies(BuiltInProfiles.COW).orElseThrow().preference();

    @Test
    void rollsStayInTypicalOrOutlierRanges() {
        SeededRandomness random = new SeededRandomness(1);
        for (int i = 0; i < 10_000; i++) {
            int value = PreferenceRoller.roll(COW, random).value();
            boolean typical = value >= 3 && value <= 8;
            boolean outlier = (value >= 1 && value <= 2) || (value >= 10 && value <= 14);
            assertTrue(typical || outlier, "unexpected roll " + value);
        }
    }

    @Test
    void mostRollsLandNearTheMeanAndSomeAreOutliers() {
        SeededRandomness random = new SeededRandomness(2);
        int nearMean = 0;
        int outliers = 0;
        int n = 10_000;
        for (int i = 0; i < n; i++) {
            int value = PreferenceRoller.roll(COW, random).value();
            if (value >= 4 && value <= 6) {
                nearMean++;
            }
            if (value <= 2 || value >= 10) {
                outliers++;
            }
        }
        assertTrue(nearMean > n / 2, "expected most rolls in 4..6, got " + nearMean);
        double outlierShare = outliers / (double) n;
        assertTrue(outlierShare > 0.05 && outlierShare < 0.11, "outlier share " + outlierShare);
    }

    @Test
    void babiesCentreOnTheirParents() {
        PreferenceDistribution noOutliers = new PreferenceDistribution(5, 1.0, IntRange.of(3, 8), 0.0, List.of());
        SeededRandomness random = new SeededRandomness(3);
        double sum = 0;
        int n = 5_000;
        for (int i = 0; i < n; i++) {
            sum += PreferenceRoller.inherit(noOutliers, new HerdPreference(8), new HerdPreference(6), random).value();
        }
        assertEquals(7.0, sum / n, 0.15);
    }

    @Test
    void inheritedMeanIsClampedToTheTypicalRange() {
        PreferenceDistribution noOutliers = new PreferenceDistribution(5, 0.1, IntRange.of(3, 8), 0.0, List.of());
        SeededRandomness random = new SeededRandomness(4);
        HerdPreference baby = PreferenceRoller.inherit(noOutliers, new HerdPreference(14), new HerdPreference(12), random);
        assertEquals(8, baby.value());
    }
}
