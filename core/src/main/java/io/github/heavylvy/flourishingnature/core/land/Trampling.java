package io.github.heavylvy.flourishingnature.core.land;

import io.github.heavylvy.flourishingnature.core.random.Randomness;

/** Hooves wearing the ground: grass to dirt to coarse dirt to path, one step at a time. */
public final class Trampling {
    private Trampling() {}

    /**
     * What one step of a walking animal does to the ground under it.
     *
     * @param ground the block under the animal's feet
     * @param baseLit block light 8 or more there: a base, never trampled
     * @param speed the animal's horizontal speed in blocks per tick
     */
    public static TrampleChange step(
            Ground ground, boolean baseLit, double speed, TrampleSettings settings, Randomness random) {
        if (baseLit || speed < TrampleSettings.MIN_SPEED) {
            return TrampleChange.NONE;
        }
        double factor = Math.min(settings.maxSpeedFactor(), speed / settings.referenceSpeed());
        return switch (ground) {
            case GRASS_BLOCK -> settings.grassToDirt().scaled(factor).roll(random)
                    ? TrampleChange.GRASS_TO_DIRT
                    : TrampleChange.NONE;
            case DIRT -> settings.dirtToCoarse().scaled(factor).roll(random)
                    ? TrampleChange.DIRT_TO_COARSE_DIRT
                    : TrampleChange.NONE;
            case COARSE_DIRT -> settings.coarseToPath().scaled(factor).roll(random)
                    ? TrampleChange.COARSE_DIRT_TO_PATH
                    : TrampleChange.NONE;
            case DIRT_PATH, MOSS_BLOCK, MOSSY_COBBLESTONE, MOSSY_GRAVEL, OTHER -> TrampleChange.NONE;
        };
    }
}
