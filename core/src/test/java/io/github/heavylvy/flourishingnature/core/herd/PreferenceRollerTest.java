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
    void eightyPercentRollTheAverageAndTenPercentAreOneOff() {
        SeededRandomness random = new SeededRandomness(2);
        int exact = 0;
        int oneOff = 0;
        int outliers = 0;
        int n = 20_000;
        for (int i = 0; i < n; i++) {
            int value = PreferenceRoller.roll(COW, random).value();
            if (value == 5) {
                exact++;
            } else if (value == 4 || value == 6) {
                oneOff++;
            } else if (value <= 2 || value >= 10) {
                outliers++;
            }
        }
        assertEquals(0.80, exact / (double) n, 0.01);
        assertEquals(0.10, oneOff / (double) n, 0.01);
        assertEquals(0.03, outliers / (double) n, 0.01);
    }

    @Test
    void babiesCentreOnTheirParents() {
        PreferenceDistribution noOutliers = new PreferenceDistribution(5, IntRange.of(3, 8), 0.0, List.of());
        SeededRandomness random = new SeededRandomness(3);
        int seven = 0;
        int n = 5_000;
        for (int i = 0; i < n; i++) {
            if (PreferenceRoller.inherit(noOutliers, new HerdPreference(8), new HerdPreference(6), random).value() == 7) {
                seven++;
            }
        }
        assertEquals(0.80, seven / (double) n, 0.02);
    }

    @Test
    void inheritedMeanIsClampedToTheTypicalRange() {
        PreferenceDistribution noOutliers = new PreferenceDistribution(5, IntRange.of(3, 8), 0.0, List.of());
        SeededRandomness random = new SeededRandomness(4);
        for (int i = 0; i < 200; i++) {
            int baby = PreferenceRoller.inherit(noOutliers, new HerdPreference(14), new HerdPreference(12), random).value();
            assertTrue(baby >= 3 && baby <= 8, "baby " + baby);
        }
    }
}
