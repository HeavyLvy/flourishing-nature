package io.github.heavylvy.flourishingnature.core.predator;

/**
 * Tuning for wild wolf packs. Distances in blocks, times in ticks (a day is 24,000).
 *
 * @param scentRadius a hungry pack notices prey this far from its home
 * @param driftStep how far a hungry pack moves its home toward prey each planning pass
 * @param driftStopWithin the pack stops drifting once its home is this close to the prey
 * @param shareRadius pack mates this close to a kill share the meal
 * @param breedCooldown a wolf that had a pup waits this long before the next
 * @param maxBreedingPackSize a pack this big or bigger has no more pups
 * @param splitAfter a starving pack this long without a kill breaks up
 * @param dispersalMin each wolf of a broken pack heads at least this far away
 * @param dispersalMax and at most this far
 * @param dispersalTime it gives up travelling after this long and settles where it is
 */
public record PackSettings(
        double scentRadius,
        double driftStep,
        double driftStopWithin,
        double shareRadius,
        long breedCooldown,
        int maxBreedingPackSize,
        long splitAfter,
        double dispersalMin,
        double dispersalMax,
        long dispersalTime) {

    private static final long DAY = 24_000L;

    public static final PackSettings DEFAULT =
            new PackSettings(48, 4, 6, 24, 3 * DAY, 6, 4 * DAY, 64, 112, DAY);

    public PackSettings {
        if (!(scentRadius > 0) || !(driftStep > 0) || !(driftStopWithin >= 0) || !(shareRadius > 0)
                || breedCooldown < 0 || maxBreedingPackSize < 2 || splitAfter < 1
                || !(dispersalMin > 0) || !(dispersalMax >= dispersalMin) || dispersalTime < 1) {
            throw new IllegalArgumentException("bad pack settings");
        }
    }
}
