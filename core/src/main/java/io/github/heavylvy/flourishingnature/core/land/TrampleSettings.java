package io.github.heavylvy.flourishingnature.core.land;

/**
 * Trampling chances per check of a walking animal, at the reference speed. A faster animal
 * covers more blocks between checks, so its chance scales with speed, up to a cap.
 *
 * <p>"Busy" needs no bookkeeping: a block has to be hit again and again to go grass, dirt, coarse
 * dirt, path, and vanilla grass spreads back onto plain dirt in between, so only spots walked
 * more often than grass regrows turn into trails.
 *
 * @param grassToDirt a grass block wears to dirt
 * @param dirtToCoarse dirt packs into coarse dirt
 * @param coarseToPath coarse dirt wears into a dirt path
 * @param referenceSpeed horizontal speed (blocks per tick) the chances are given for: a walk
 * @param maxSpeedFactor most a gallop can multiply the chances by
 */
public record TrampleSettings(
        Chance grassToDirt, Chance dirtToCoarse, Chance coarseToPath, double referenceSpeed, double maxSpeedFactor) {

    /** Animals barely moving (grazing, shuffling) don't trample. In blocks per tick. */
    public static final double MIN_SPEED = 0.02;

    public TrampleSettings {
        if (!(referenceSpeed > 0.0)) {
            throw new IllegalArgumentException("reference speed must be > 0: " + referenceSpeed);
        }
        if (!(maxSpeedFactor >= 1.0)) {
            throw new IllegalArgumentException("max speed factor must be >= 1: " + maxSpeedFactor);
        }
    }

    public static final TrampleSettings DEFAULT =
            new TrampleSettings(new Chance(0.0002), new Chance(0.001), new Chance(0.0003), 0.1, 4.0);

    public static final TrampleSettings ALWAYS =
            new TrampleSettings(Chance.ALWAYS, Chance.ALWAYS, Chance.ALWAYS, 0.1, 4.0);
}
