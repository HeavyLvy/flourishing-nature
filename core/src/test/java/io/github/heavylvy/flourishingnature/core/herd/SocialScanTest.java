package io.github.heavylvy.flourishingnature.core.herd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class SocialScanTest {
    private static final SpeciesProfile COW = BuiltInProfiles.forSpecies(BuiltInProfiles.COW).orElseThrow();
    private static final HerdingSettings SETTINGS = HerdingSettings.DEFAULT;
    private static final HerdMember SELF = new HerdMember(COW, new HerdPreference(5), new Position(0, 64, 0));

    private static Neighbor neighbor(SpeciesId species, int preference, double x) {
        return new Neighbor(species, new HerdPreference(preference), new Position(x, 64, 0));
    }

    @Test
    void identicalCowsCountAsFullMates() {
        List<Neighbor> cows = List.of(neighbor(BuiltInProfiles.COW, 5, 2), neighbor(BuiltInProfiles.COW, 5, 3));
        assertEquals(2.0, SocialScan.socialCount(SELF, cows, SETTINGS), 1.0e-9);
    }

    @Test
    void otherSpeciesCountByAffinityAndUnrelatedOnesNotAtAll() {
        List<Neighbor> mixed = List.of(neighbor(BuiltInProfiles.SHEEP, 5, 2), neighbor(BuiltInProfiles.PIG, 5, 2));
        assertEquals(0.4, SocialScan.socialCount(SELF, mixed, SETTINGS), 1.0e-9);
    }

    @Test
    void neighborsBeyondTheHerdRadiusDoNotCount() {
        assertEquals(0.0, SocialScan.socialCount(SELF, List.of(neighbor(BuiltInProfiles.COW, 5, 10)), SETTINGS));
    }

    @Test
    void likeMindedMobsWeighMore() {
        HerdMember social = new HerdMember(COW, new HerdPreference(8), new Position(0, 64, 0));
        double similar = SocialWeight.between(social, neighbor(BuiltInProfiles.COW, 8, 1), SETTINGS);
        double different = SocialWeight.between(social, neighbor(BuiltInProfiles.COW, 2, 1), SETTINGS);
        assertEquals(1.0, similar, 1.0e-9);
        assertTrue(different < similar && different > 0.0, "different-minded weight " + different);
    }
}
