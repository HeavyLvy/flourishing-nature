package io.github.heavylvy.flourishingnature.core.herd;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class CohesionTest {
    private static final SpeciesProfile COW = BuiltInProfiles.forSpecies(BuiltInProfiles.COW).orElseThrow();
    private static final HerdMember SELF = new HerdMember(COW, new HerdPreference(5), new Position(0, 64, 0));

    private static Neighbor cowAt(double x) {
        return new Neighbor(BuiltInProfiles.COW, new HerdPreference(5), new Position(x, 64, 0));
    }

    @Test
    void comfortableSpacingNeedsNoMove() {
        assertTrue(Cohesion.adjust(SELF, List.of(cowAt(3)), HerdingSettings.DEFAULT).isEmpty());
    }

    @Test
    void tooCloseStepsAway() {
        Optional<Position> move = Cohesion.adjust(SELF, List.of(cowAt(1)), HerdingSettings.DEFAULT);
        assertTrue(move.map(p -> p.x() < 0).orElse(false), "expected a step to -x, got " + move);
    }

    @Test
    void tooFarClosesUp() {
        Optional<Position> move = Cohesion.adjust(SELF, List.of(cowAt(6), cowAt(7)), HerdingSettings.DEFAULT);
        assertTrue(move.map(p -> p.x() > 5).orElse(false), "expected a move toward +x, got " + move);
    }

    @Test
    void noMatesNoMove() {
        assertTrue(Cohesion.adjust(SELF, List.of(), HerdingSettings.DEFAULT).isEmpty());
    }
}
