package io.github.heavylvy.flourishingnature.core.weather;

import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.herd.Terrain;
import io.github.heavylvy.flourishingnature.core.random.Randomness;
import java.util.Optional;
import java.util.function.Function;

/**
 * A herd caught in the rain with no cover nearby goes looking for some, together. Each step it
 * scouts eight directions; if any scout spots cover, the herd heads that way, otherwise it keeps
 * walking roughly the way it was going (turning when blocked by water or cliffs), like hikers
 * heading for the treeline in a storm.
 *
 * @param heading direction of travel in radians
 */
public record Expedition(double heading) {
    public static final double STEP = 12.0;
    private static final int DIRECTIONS = 8;

    public static Expedition start(Randomness random) {
        return new Expedition(random.nextDouble() * 2 * Math.PI);
    }

    /** Where the herd's home moves next, and the heading after the move. */
    public record Leg(Position anchor, Expedition next) {}

    /**
     * Plans one leg of the search from {@code anchor}.
     *
     * @param shelterNear finds cover near a scouted point, if any
     * @param terrain where a herd home may stand
     */
    public Optional<Leg> step(Position anchor, Function<Position, Optional<Position>> shelterNear, Terrain terrain) {
        Optional<Leg> best = Optional.empty();
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < DIRECTIONS; i++) {
            double angle = heading + i * 2 * Math.PI / DIRECTIONS;
            Position scout = toward(anchor, angle, STEP);
            Optional<Position> cover = shelterNear.apply(scout);
            if (cover.isPresent() && cover.get().distanceTo(anchor) < bestDistance) {
                Optional<Position> standHere = terrain.pasture(towardPoint(anchor, cover.get(), STEP));
                if (standHere.isPresent()) {
                    bestDistance = cover.get().distanceTo(anchor);
                    best = Optional.of(new Leg(standHere.get(), new Expedition(angle)));
                }
            }
        }
        if (best.isPresent()) {
            return best;
        }
        double[] turns = {0, Math.PI / 4, -Math.PI / 4, Math.PI / 2, -Math.PI / 2, 3 * Math.PI / 4, -3 * Math.PI / 4, Math.PI};
        for (double turn : turns) {
            Optional<Position> next = terrain.pasture(toward(anchor, heading + turn, STEP));
            if (next.isPresent()) {
                return Optional.of(new Leg(next.get(), new Expedition(heading + turn)));
            }
        }
        return Optional.empty();
    }

    private static Position toward(Position from, double angle, double distance) {
        return new Position(from.x() + Math.cos(angle) * distance, from.y(), from.z() + Math.sin(angle) * distance);
    }

    /** A point at most {@code distance} from {@code from} on the way to {@code to}. */
    private static Position towardPoint(Position from, Position to, double distance) {
        double d = from.distanceTo(to);
        if (d <= distance) {
            return to;
        }
        double t = distance / d;
        return new Position(from.x() + (to.x() - from.x()) * t, from.y() + (to.y() - from.y()) * t, from.z() + (to.z() - from.z()) * t);
    }
}
