package io.github.heavylvy.flourishingnature.core.nest;

/**
 * Tunables for nests. All distances in blocks, times in game ticks.
 *
 * @param claimRadius how far a hen without a nest looks for an empty one to claim
 * @param gatherRadius how far she looks for nest material
 * @param buildPoints material points needed to build a nest
 * @param incubationTicks ticks of sitting a fertile egg needs to hatch
 * @param wildDecayTicks how long a wild nest lasts with no hen around before it falls apart
 * @param layIntervalTicks ticks between eggs for a content, fed hen
 * @param broodShare chance a hen with fertile eggs starts a sitting bout when she is free
 * @param broodBoutTicks how long one sitting bout lasts
 */
public record NestSettings(
        int claimRadius,
        int gatherRadius,
        int buildPoints,
        long incubationTicks,
        long wildDecayTicks,
        long layIntervalTicks,
        double broodShare,
        int broodBoutTicks) {

    /** The spec's starting numbers. Vanilla chickens lay every 6000 to 12000 ticks. */
    public static final NestSettings DEFAULT = new NestSettings(16, 12, 8, 12000, 72000, 9000, 0.75, 1200);

    public NestSettings {
        if (claimRadius < 1 || gatherRadius < 1 || buildPoints < 1) {
            throw new IllegalArgumentException("radii and build points must be >= 1");
        }
        if (incubationTicks < 1 || wildDecayTicks < 1 || layIntervalTicks < 1 || broodBoutTicks < 1) {
            throw new IllegalArgumentException("times must be >= 1 tick");
        }
        if (!(broodShare >= 0.0 && broodShare <= 1.0)) {
            throw new IllegalArgumentException("brood share must be in [0, 1]: " + broodShare);
        }
    }
}
