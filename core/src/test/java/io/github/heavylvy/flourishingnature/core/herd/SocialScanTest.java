package io.github.heavylvy.flourishingnature.core.herd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SocialScanTest {
    private static final SpeciesProfile COW = BuiltInProfiles.forSpecies(BuiltInProfiles.COW).orElseThrow();
    private static final HerdingSettings SETTINGS = HerdingSettings.DEFAULT;

    private static HerdMember cow(int preference, double x, double z) {
        return new HerdMember(COW, new HerdPreference(preference), new Position(x, 64, z));
    }

    private static Neighbor neighbor(SpeciesId species, int preference, double x, double z) {
        return new Neighbor(species, new HerdPreference(preference), new Position(x, 64, z));
    }

    private static List<Neighbor> cowsAround(double x, double z, int count, int preference) {
        List<Neighbor> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(neighbor(BuiltInProfiles.COW, preference, x + (i % 3) * 2.0, z + Math.floorDiv(i, 3) * 2.0));
        }
        return result;
    }

    @Test
    void identicalCowsCountAsFullMates() {
        SocialAssessment result = SocialScan.assess(cow(5, 0, 0), cowsAround(2, 0, 4, 5), SETTINGS);
        assertEquals(4.0, result.socialCount(), 1.0e-9);
        assertInstanceOf(HerdDecision.Content.class, result.decision());
    }

    @Test
    void lonelyCowSeeksTheBiggestNearbyHerd() {
        List<Neighbor> neighbors = new ArrayList<>();
        neighbors.addAll(cowsAround(12, 0, 2, 5));
        neighbors.addAll(cowsAround(-12, 0, 5, 5));
        SocialAssessment result = SocialScan.assess(cow(6, 0, 0), neighbors, SETTINGS);
        HerdDecision.SeekHerd seek = assertInstanceOf(HerdDecision.SeekHerd.class, result.decision());
        assertTrue(seek.target().x() < 0, "should head for the bigger herd at -x, went to " + seek.target());
    }

    @Test
    void lonerIsContentAlone() {
        SocialAssessment result = SocialScan.assess(cow(1, 0, 0), cowsAround(12, 0, 5, 5), SETTINGS);
        assertInstanceOf(HerdDecision.Content.class, result.decision());
    }

    @Test
    void crowdedCowDriftsAwayFromTheCentre() {
        SocialAssessment result = SocialScan.assess(cow(2, 0, 0), cowsAround(1, 0, 6, 2), SETTINGS);
        HerdDecision.Disperse disperse = assertInstanceOf(HerdDecision.Disperse.class, result.decision());
        assertTrue(disperse.target().x() < 0, "should move away from the crowd at +x, went to " + disperse.target());
    }

    @Test
    void cowWithNobodyInSightIsLonely() {
        SocialAssessment result = SocialScan.assess(cow(5, 0, 0), List.of(), SETTINGS);
        assertInstanceOf(HerdDecision.Lonely.class, result.decision());
        assertEquals(0.0, result.socialCount());
    }

    @Test
    void cowAlreadyInTheOnlyHerdDoesNotChaseItsOwnCentre() {
        SocialAssessment result = SocialScan.assess(cow(8, 0, 0), cowsAround(2, 0, 3, 8), SETTINGS);
        assertInstanceOf(HerdDecision.Lonely.class, result.decision());
    }

    @Test
    void otherSpeciesCountByAffinity() {
        List<Neighbor> sheep = List.of(
                neighbor(BuiltInProfiles.SHEEP, 5, 2, 0),
                neighbor(BuiltInProfiles.SHEEP, 5, 3, 0));
        SocialAssessment result = SocialScan.assess(cow(5, 0, 0), sheep, SETTINGS);
        assertEquals(0.8, result.socialCount(), 1.0e-9);
    }

    @Test
    void unrelatedSpeciesAreIgnored() {
        List<Neighbor> pigs = List.of(neighbor(BuiltInProfiles.PIG, 5, 2, 0));
        SocialAssessment result = SocialScan.assess(cow(5, 0, 0), pigs, SETTINGS);
        assertEquals(0.0, result.socialCount());
    }

    @Test
    void likeMindedMobsWeighMore() {
        HerdMember social = cow(8, 0, 0);
        double similar = SocialWeight.between(social, neighbor(BuiltInProfiles.COW, 8, 1, 0), SETTINGS);
        double different = SocialWeight.between(social, neighbor(BuiltInProfiles.COW, 2, 1, 0), SETTINGS);
        assertEquals(1.0, similar, 1.0e-9);
        assertTrue(different < similar && different > 0.0, "different-minded weight " + different);
    }

    @Test
    void neighborsBeyondTheHerdRadiusDoNotCount() {
        SocialAssessment result = SocialScan.assess(cow(5, 0, 0), cowsAround(10, 0, 3, 5), SETTINGS);
        assertEquals(0.0, result.socialCount());
        assertInstanceOf(HerdDecision.SeekHerd.class, result.decision());
    }
}
