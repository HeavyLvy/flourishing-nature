package io.github.heavylvy.flourishingnature.core.herbivore;

import io.github.heavylvy.flourishingnature.core.herd.HerdHome;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.herd.Terrain;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Moves a herd's home away from danger and toward food. Runs after the herd planner, so it only
 * nudges the anchor; the planner's own roaming and spacing keep working.
 */
public final class HomeChoice {
    private HomeChoice() {}

    /** Compass directions tried when stepping out of a danger area. */
    private static final int ESCAPE_DIRECTIONS = 8;
    /** Land a little past the edge so the next pass doesn't bounce straight back in. */
    private static final double ESCAPE_MARGIN = 2.0;

    /**
     * A home outside every remembered danger area: the reachable spot just past the edge of the
     * area it is in, nearest the current anchor. Unchanged if it is safe already or there's nowhere
     * safe to go.
     */
    public static HerdHome avoidDanger(HerdHome home, List<DangerSpot> dangers, long now, Terrain terrain, DangerSettings settings) {
        Optional<DangerSpot> inside = dangers.stream()
                .filter(d -> d.covers(home.anchor(), now, settings))
                .max(Comparator.comparingDouble(d -> d.radius(now, settings) - d.horizontalDistance(home.anchor())));
        if (inside.isEmpty()) {
            return home;
        }
        DangerSpot spot = inside.get();
        double distance = spot.radius(now, settings) + ESCAPE_MARGIN;
        List<Position> options = new ArrayList<>();
        for (int i = 0; i < ESCAPE_DIRECTIONS; i++) {
            double angle = 2 * Math.PI * i / ESCAPE_DIRECTIONS;
            Position wanted = new Position(
                    spot.where().x() + Math.cos(angle) * distance, home.anchor().y(), spot.where().z() + Math.sin(angle) * distance);
            terrain.pasture(wanted).filter(p -> safe(p, dangers, now, settings)).ifPresent(options::add);
        }
        return options.stream()
                .min(Comparator.comparingDouble(p -> p.distanceSquaredTo(home.anchor())))
                .map(p -> new HerdHome(p, home.roamer(), home.nextMove()))
                .orElse(home);
    }

    /**
     * When the food around home has run out, moves home to the richest patch nearby that is safe
     * and has more food than home. Ties go to the nearer patch.
     */
    public static HerdHome followFood(
            HerdHome home,
            int foodAtHome,
            List<FoodPatch> patches,
            List<DangerSpot> dangers,
            long now,
            Terrain terrain,
            DangerSettings danger,
            HomeSettings settings) {
        if (foodAtHome > settings.scarceAt()) {
            return home;
        }
        return patches.stream()
                .filter(p -> p.food() > foodAtHome && p.food() > settings.scarceAt())
                .filter(p -> safe(p.centre(), dangers, now, danger))
                .sorted(Comparator.comparingInt(FoodPatch::food).reversed()
                        .thenComparingDouble(p -> p.centre().distanceSquaredTo(home.anchor())))
                .flatMap(p -> terrain.pasture(p.centre()).stream())
                .findFirst()
                .map(p -> new HerdHome(p, home.roamer(), home.nextMove()))
                .orElse(home);
    }

    /** Patch centres to look at: rings around home at each of the settings' radii. */
    public static List<Position> patchCentres(Position home, HomeSettings settings) {
        List<Position> centres = new ArrayList<>();
        for (double r : settings.patchRadii()) {
            for (int i = 0; i < ESCAPE_DIRECTIONS; i++) {
                double angle = 2 * Math.PI * i / ESCAPE_DIRECTIONS;
                centres.add(new Position(home.x() + Math.cos(angle) * r, home.y(), home.z() + Math.sin(angle) * r));
            }
        }
        return centres;
    }

    static boolean safe(Position p, List<DangerSpot> dangers, long now, DangerSettings settings) {
        return dangers.stream().noneMatch(d -> d.covers(p, now, settings));
    }
}
