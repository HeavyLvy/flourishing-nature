package io.github.heavylvy.flourishingnature.core.herd;

import io.github.heavylvy.flourishingnature.core.random.Randomness;
import java.util.Optional;
import java.util.Set;

/**
 * A herd breaking into a run across open country, like wild horses on a plain: a roughly straight
 * line with a gentle wobble, for 100 to 300 blocks, swerving around water and cliffs and stopping
 * when the way is blocked.
 *
 * @param heading current direction of travel, radians
 * @param remaining blocks left to run
 */
public record Gallop(double heading, double remaining) {
    public static final double MIN_DISTANCE = 100.0;
    public static final double MAX_DISTANCE = 300.0;
    /** How far the herd's home moves ahead per step (one step per planning pass, 2 seconds). */
    public static final double STEP = 14.0;
    /** Most the heading drifts per step, either way: enough to curve, not enough to zigzag. */
    static final double WOBBLE = Math.toRadians(8);

    private static final Set<SpeciesId> RUNNERS = Set.of(SpeciesId.minecraft("horse"), SpeciesId.minecraft("donkey"));

    /** Species whose herds go for runs. */
    public static boolean runs(SpeciesId species) {
        return RUNNERS.contains(species);
    }

    public Gallop {
        if (!(remaining >= 0.0) || !Double.isFinite(heading)) {
            throw new IllegalArgumentException("bad gallop: heading " + heading + ", remaining " + remaining);
        }
    }

    public static Gallop start(Randomness random) {
        return start(random.nextDouble() * 2 * Math.PI, random);
    }

    public static Gallop start(double heading, Randomness random) {
        return new Gallop(heading, MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE));
    }

    /** Where the herd's home moves next, and what is left of the run. */
    public record Stride(Position anchor, Gallop next) {}

    /** The next stride from {@code anchor}, or empty when the run is over or blocked. */
    public Optional<Stride> step(Position anchor, Terrain terrain, Randomness random) {
        if (remaining <= 0.0) {
            return Optional.empty();
        }
        double wobbled = heading + (random.nextDouble() * 2 - 1) * WOBBLE;
        double length = Math.min(STEP, remaining);
        for (double swerve : new double[] {0, Math.toRadians(25), -Math.toRadians(25), Math.toRadians(50), -Math.toRadians(50)}) {
            double h = wobbled + swerve;
            Optional<Position> next = terrain.pasture(new Position(
                    anchor.x() + Math.cos(h) * length, anchor.y(), anchor.z() + Math.sin(h) * length));
            if (next.isPresent()) {
                return Optional.of(new Stride(next.get(), new Gallop(h, remaining - length)));
            }
        }
        return Optional.empty();
    }
}
