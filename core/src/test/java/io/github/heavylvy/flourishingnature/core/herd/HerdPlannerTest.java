package io.github.heavylvy.flourishingnature.core.herd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

final class HerdPlannerTest {
    private static final SpeciesProfile COW = BuiltInProfiles.forSpecies(BuiltInProfiles.COW).orElseThrow();
    private static final SpeciesProfile PIG = BuiltInProfiles.forSpecies(BuiltInProfiles.PIG).orElseThrow();
    private static final HerdPlanSettings S = HerdPlanSettings.DEFAULT;

    /** Dry grass everywhere except a lake where x is below 0. */
    private static final Terrain LAKE_TO_THE_WEST = near -> near.x() < 0 ? Optional.empty() : Optional.of(near);

    private static Supplier<HerdId> ids() {
        long[] next = {1};
        return () -> new HerdId(new UUID(0, next[0]++));
    }

    private static HerdAnimal cow(int handle, int preference, double x, double z, Membership membership) {
        return new HerdAnimal(handle, COW, new HerdPreference(preference), new Position(x, 64, z), membership);
    }

    private static HerdPlan plan(List<HerdAnimal> animals, Map<HerdId, HerdHome> homes) {
        return HerdPlanner.plan(animals, homes, 0, new SeededRandomness(1), LAKE_TO_THE_WEST, ids(), S);
    }

    private static Set<HerdId> herdsOf(HerdPlan plan) {
        return new HashSet<>(plan.assignments().values());
    }

    private static final Membership NONE = new Membership.Unassigned();

    @Test
    void cowsNearEachOtherFormOneHerd() {
        List<HerdAnimal> cows = List.of(cow(1, 5, 10, 10, NONE), cow(2, 5, 14, 10, NONE), cow(3, 5, 10, 14, NONE));
        assertEquals(1, herdsOf(plan(cows, Map.of())).size());
    }

    @Test
    void lonerStaysAlone() {
        List<HerdAnimal> cows = List.of(cow(1, 1, 10, 10, NONE), cow(2, 5, 12, 10, NONE), cow(3, 5, 10, 12, NONE));
        HerdPlan plan = plan(cows, Map.of());
        assertEquals(2, herdsOf(plan).size());
        assertNotEquals(plan.assignments().get(1), plan.assignments().get(2));
    }

    @Test
    void twoSmallNeighbouringHerdsMerge() {
        HerdId a = new HerdId(new UUID(9, 1));
        HerdId b = new HerdId(new UUID(9, 2));
        List<HerdAnimal> cows = List.of(
                cow(1, 5, 10, 10, new Membership.InHerd(a)), cow(2, 5, 11, 10, new Membership.InHerd(a)),
                cow(3, 5, 22, 10, new Membership.InHerd(b)), cow(4, 5, 23, 10, new Membership.InHerd(b)));
        Map<HerdId, HerdHome> homes = Map.of(
                a, new HerdHome(new Position(10.5, 64, 10), false, 99_999),
                b, new HerdHome(new Position(22.5, 64, 10), false, 99_999));
        assertEquals(1, herdsOf(plan(cows, homes)).size());
    }

    @Test
    void twoFullHerdsSideBySideStaySeparate() {
        HerdId a = new HerdId(new UUID(9, 1));
        HerdId b = new HerdId(new UUID(9, 2));
        List<HerdAnimal> cows = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            cows.add(cow(i, 5, 10 + i, 10, new Membership.InHerd(a)));
            cows.add(cow(10 + i, 5, 10 + i, 18, new Membership.InHerd(b)));
        }
        Map<HerdId, HerdHome> homes = Map.of(
                a, new HerdHome(new Position(12, 64, 10), false, 99_999),
                b, new HerdHome(new Position(12, 64, 18), false, 99_999));
        assertEquals(Set.of(a, b), herdsOf(plan(cows, homes)));
    }

    @Test
    void crowdedMembersSplitOffTogether() {
        HerdId big = new HerdId(new UUID(9, 1));
        List<HerdAnimal> cows = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            cows.add(cow(i, 6, 10 + i, 10, new Membership.InHerd(big)));
        }
        cows.add(cow(100, 3, 10, 11, new Membership.InHerd(big)));
        cows.add(cow(101, 3, 11, 11, new Membership.InHerd(big)));
        cows.add(cow(102, 3, 12, 11, new Membership.InHerd(big)));
        HerdPlan plan = plan(cows, Map.of(big, new HerdHome(new Position(12, 64, 10), false, 99_999)));
        HerdId splinter = plan.assignments().get(100);
        assertNotEquals(big, splinter, "the crowded cows should have left");
        assertEquals(splinter, plan.assignments().get(101));
        assertEquals(splinter, plan.assignments().get(102));
        assertEquals(big, plan.assignments().get(0));
    }

    @Test
    void aSingleCrowdedMemberMovesToARoomierHerdInsteadOfLeavingAlone() {
        HerdId full = new HerdId(new UUID(9, 1));
        HerdId roomy = new HerdId(new UUID(9, 2));
        List<HerdAnimal> cows = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            cows.add(cow(i, 6, 10 + i, 10, new Membership.InHerd(full)));
        }
        cows.add(cow(100, 4, 10, 11, new Membership.InHerd(full)));
        cows.add(cow(200, 4, 30, 10, new Membership.InHerd(roomy)));
        cows.add(cow(201, 4, 31, 10, new Membership.InHerd(roomy)));
        Map<HerdId, HerdHome> homes = Map.of(
                full, new HerdHome(new Position(13, 64, 10), false, 99_999),
                roomy, new HerdHome(new Position(30.5, 64, 10), false, 99_999));
        HerdPlan plan = plan(cows, homes);
        assertEquals(roomy, plan.assignments().get(100));
    }

    @Test
    void aSingleCrowdedMemberWithNowhereToGoStays() {
        HerdId full = new HerdId(new UUID(9, 1));
        List<HerdAnimal> cows = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            cows.add(cow(i, 6, 10 + i, 10, new Membership.InHerd(full)));
        }
        cows.add(cow(100, 4, 10, 11, new Membership.InHerd(full)));
        HerdPlan plan = plan(cows, Map.of(full, new HerdHome(new Position(13, 64, 10), false, 99_999)));
        assertEquals(full, plan.assignments().get(100));
    }

    @Test
    void cowsAndPigsKeepSeparateHerds() {
        List<HerdAnimal> animals = List.of(
                cow(1, 3, 10, 10, NONE),
                new HerdAnimal(2, PIG, new HerdPreference(3), new Position(11, 64, 10), NONE));
        assertEquals(2, herdsOf(plan(animals, Map.of())).size());
    }

    @Test
    void homesAvoidTheLake() {
        List<HerdAnimal> cows = List.of(cow(1, 5, -3, 10, NONE), cow(2, 5, -2, 10, NONE));
        HerdPlan plan = plan(cows, Map.of());
        for (HerdHome home : plan.homes().values()) {
            assertTrue(home.anchor().x() >= 0, "home in the lake at " + home.anchor());
        }
    }

    @Test
    void roamingHerdsMoveTheirHomeWhenItIsTime() {
        HerdId herd = new HerdId(new UUID(9, 1));
        List<HerdAnimal> cows = List.of(cow(1, 5, 50, 50, new Membership.InHerd(herd)));
        HerdHome home = new HerdHome(new Position(50, 64, 50), true, 100);
        HerdPlan plan = HerdPlanner.plan(cows, Map.of(herd, home), 100, new SeededRandomness(2), LAKE_TO_THE_WEST, ids(), S);
        HerdHome moved = Optional.ofNullable(plan.homes().get(herd)).orElseThrow();
        assertTrue(moved.anchor().distanceTo(home.anchor()) > 10, "roamer moved only to " + moved.anchor());
        assertTrue(moved.nextMove() > 100);
    }

    @Test
    void lostAnimalsLeaveAndJoinWhateverIsNear() {
        HerdId herd = new HerdId(new UUID(9, 1));
        List<HerdAnimal> cows = List.of(cow(1, 5, 10, 10, new Membership.InHerd(herd)), cow(2, 5, 200, 10, new Membership.InHerd(herd)));
        HerdPlan plan = plan(cows, Map.of(herd, new HerdHome(new Position(10, 64, 10), false, 99_999)));
        assertEquals(herd, plan.assignments().get(1));
        assertNotEquals(herd, plan.assignments().get(2));
    }

    @Test
    void repeatedPlanningIsStable() {
        List<HerdAnimal> cows = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            cows.add(cow(i, 5, 10 + i, 10, NONE));
        }
        HerdPlan first = plan(cows, Map.of());
        Map<HerdId, HerdHome> homes = new HashMap<>(first.homes());
        List<HerdAnimal> again = new ArrayList<>();
        for (HerdAnimal c : cows) {
            again.add(new HerdAnimal(c.handle(), c.profile(), c.preference(), c.position(),
                    new Membership.InHerd(Optional.ofNullable(first.assignments().get(c.handle())).orElseThrow())));
        }
        HerdPlan second = plan(again, homes);
        assertEquals(first.assignments(), second.assignments());
    }
}
