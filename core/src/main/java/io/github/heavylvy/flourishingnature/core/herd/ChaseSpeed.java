package io.github.heavylvy.flourishingnature.core.herd;

/**
 * Keeps hunts fair: prey run a little slower than the hunter chasing them, so a hunt can succeed
 * but takes a real chase, like a cheetah gaining on a gazelle rather than teleporting to it.
 */
public final class ChaseSpeed {
    private ChaseSpeed() {}

    /** Prey flee at this share of the hunter's chase speed. */
    public static final double PREY_SHARE = 0.9;

    /**
     * Speed multiplier for prey to use while fleeing.
     *
     * @param preyBaseSpeed the prey's own movement speed, blocks per tick
     * @param chaserSpeed the hunter's chase speed, blocks per tick, or infinity if not a hunter
     * @param panicModifier the multiplier the prey would use without a hunter to match
     */
    public static double fleeModifier(double preyBaseSpeed, double chaserSpeed, double panicModifier) {
        if (!(preyBaseSpeed > 0.0)) {
            throw new IllegalArgumentException("prey speed must be above 0: " + preyBaseSpeed);
        }
        if (Double.isInfinite(chaserSpeed)) {
            return panicModifier;
        }
        return Math.min(panicModifier, PREY_SHARE * chaserSpeed / preyBaseSpeed);
    }
}
