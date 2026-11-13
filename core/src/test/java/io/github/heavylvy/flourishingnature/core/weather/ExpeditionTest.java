package io.github.heavylvy.flourishingnature.core.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.herd.Terrain;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class ExpeditionTest {
    private static final Terrain OPEN = Optional::of;
    private static final Position HOME = new Position(0, 64, 0);

    @Test
    void headsForCoverAScoutSpotted() {
        Position grove = new Position(-15, 64, 0);
        Expedition.Leg leg = new Expedition(0).step(HOME,
                        scout -> scout.distanceTo(grove) < 6 ? Optional.of(grove) : Optional.empty(), OPEN)
                .orElseThrow();
        assertTrue(leg.anchor().x() < -10, "went to " + leg.anchor());
    }

    @Test
    void keepsGoingStraightWithNothingInSight() {
        Expedition.Leg leg = new Expedition(0).step(HOME, scout -> Optional.empty(), OPEN).orElseThrow();
        assertEquals(Expedition.STEP, leg.anchor().x(), 1.0e-9);
        assertEquals(0.0, leg.next().heading(), 1.0e-9);
    }

    @Test
    void turnsAwayFromALakeAhead() {
        Terrain lakeToTheEast = p -> p.x() > 5 ? Optional.empty() : Optional.of(p);
        Expedition.Leg leg = new Expedition(0).step(HOME, scout -> Optional.empty(), lakeToTheEast).orElseThrow();
        assertTrue(leg.anchor().x() <= 5, "walked into the lake at " + leg.anchor());
    }
}
