package io.github.heavylvy.flourishingnature.core.herbivore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.herd.Terrain;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class HomeChoiceTest {
    private static final DangerSettings DANGER = DangerSettings.DEFAULT;
    private static final HomeSettings HOMES = HomeSettings.DEFAULT;
    private static final Terrain FLAT = near -> Optional.of(new Position(near.x(), 64, near.z()));
    private static final Terrain WATER_EAST = near -> near.x() > 0 ? Optional.empty() : FLAT.pasture(near);
    private static final HerdHome HOME = new HerdHome(new Position(0, 64, 0), false, 500L);

    @Test
    void dangerFadesOverTheSecondDay() {
        DangerSpot spot = new DangerSpot(new Position(0, 64, 0), 0L);
        assertEquals(16.0, spot.radius(24_000L, DANGER), 1.0e-9);
        assertEquals(8.0, spot.radius(36_000L, DANGER), 1.0e-9);
        assertEquals(0.0, spot.radius(48_000L, DANGER), 1.0e-9);
        assertTrue(spot.forgotten(48_000L, DANGER));
    }

    @Test
    void memoryRefreshesNearbySpotsAndKeepsAFew() {
        DangerMemory memory = DangerMemory.EMPTY
                .remember(new Position(0, 64, 0), 0L, DANGER)
                .remember(new Position(1, 64, 1), 100L, DANGER);
        assertEquals(1, memory.spots().size(), "same spot again only refreshes it");
        assertEquals(100L, memory.spots().getFirst().atTick());
        for (int i = 1; i <= 5; i++) {
            memory = memory.remember(new Position(20 * i, 64, 0), 100L + i, DANGER);
        }
        assertEquals(DANGER.maxSpots(), memory.spots().size());
        assertEquals(memory.spots().size(), memory.active(200L, DANGER).size());
        assertEquals(0, memory.active(100_000L, DANGER).size());
    }

    @Test
    void homeStepsOutOfDanger() {
        List<DangerSpot> dangers = List.of(new DangerSpot(new Position(3, 64, 0), 0L));
        HerdHome moved = HomeChoice.avoidDanger(HOME, dangers, 100L, FLAT, DANGER);
        assertTrue(new Position(3, 64, 0).distanceTo(moved.anchor()) >= 16.0, "moved out: " + moved.anchor());
        assertTrue(moved.anchor().x() < 0, "away from the spot, the short way: " + moved.anchor());
        assertEquals(HOME.nextMove(), moved.nextMove());
        assertEquals(HOME, HomeChoice.avoidDanger(HOME, dangers, 100_000L, FLAT, DANGER), "forgotten after two days");
        assertEquals(HOME, HomeChoice.avoidDanger(HOME, List.of(new DangerSpot(new Position(40, 64, 0), 0L)), 100L, FLAT, DANGER));
    }

    @Test
    void escapeSkipsUnusableGround() {
        List<DangerSpot> dangers = List.of(new DangerSpot(new Position(-3, 64, 0), 0L));
        HerdHome moved = HomeChoice.avoidDanger(HOME, dangers, 100L, WATER_EAST, DANGER);
        assertTrue(moved.anchor().x() <= 0, "never into the water: " + moved.anchor());
        assertTrue(new Position(-3, 64, 0).distanceTo(moved.anchor()) >= 16.0);
    }

    @Test
    void hungryHomeMovesToTheRichestSafePatch() {
        List<FoodPatch> patches = List.of(
                new FoodPatch(new Position(12, 64, 0), 6),
                new FoodPatch(new Position(-12, 64, 0), 9),
                new FoodPatch(new Position(0, 64, 20), 1));
        HerdHome moved = HomeChoice.followFood(HOME, 0, patches, List.of(), 0L, FLAT, DANGER, HOMES);
        assertEquals(-12.0, moved.anchor().x(), 1.0e-9);
        List<DangerSpot> westIsDangerous = List.of(new DangerSpot(new Position(-12, 64, 0), 0L));
        HerdHome safer = HomeChoice.followFood(HOME, 0, patches, westIsDangerous, 10L, FLAT, DANGER, HOMES);
        assertEquals(12.0, safer.anchor().x(), 1.0e-9);
    }

    @Test
    void wellFedHomesStay() {
        List<FoodPatch> patches = List.of(new FoodPatch(new Position(12, 64, 0), 30));
        assertEquals(HOME, HomeChoice.followFood(HOME, 5, patches, List.of(), 0L, FLAT, DANGER, HOMES));
        assertEquals(HOME, HomeChoice.followFood(HOME, 0, List.of(new FoodPatch(new Position(12, 64, 0), 2)),
                List.of(), 0L, FLAT, DANGER, HOMES), "no point moving to another bare patch");
    }

    @Test
    void patchCentresRingTheHome() {
        List<Position> centres = HomeChoice.patchCentres(HOME.anchor(), HOMES);
        assertEquals(16, centres.size());
        assertEquals(12.0, centres.getFirst().distanceTo(HOME.anchor()), 1.0e-9);
    }
}
