package io.github.heavylvy.flourishingnature.core.land;

import io.github.heavylvy.flourishingnature.core.random.Randomness;

/**
 * Decides what, if anything, changes at one sampled spot. Pure and cheap: the loader module
 * gathers a {@link Column}, asks here, and applies the answer.
 */
public final class LandRules {
    private LandRules() {}

    public static LandChange decide(Column column, LandSettings settings, Randomness random) {
        // Base protection: nothing changes in a torch-lit area.
        if (column.baseLit()) {
            return LandChange.NONE;
        }
        return switch (column.ground()) {
            case GRASS_BLOCK, DIRT -> plants(column, settings, random);
            case COARSE_DIRT -> column.tallGrassBeside() && settings.loosenCoarseDirt().roll(random)
                    ? LandChange.LOOSEN_COARSE_DIRT
                    : LandChange.NONE;
            case MOSS_BLOCK, MOSSY_COBBLESTONE -> column.humid() && growth(column, settings.creepMoss()).roll(random)
                    ? LandChange.CREEP_MOSS
                    : LandChange.NONE;
            case MOSSY_GRAVEL -> column.humid() && settings.gravelToDirt().roll(random)
                    ? LandChange.GRAVEL_TO_DIRT
                    : LandChange.NONE;
            case DIRT_PATH, OTHER -> LandChange.NONE;
        };
    }

    private static LandChange plants(Column column, LandSettings settings, Randomness random) {
        if (!column.sunlit()) {
            return LandChange.NONE;
        }
        // Bare dirt is left to vanilla, which spreads grass onto it; only grass blocks sprout.
        boolean grass = column.ground() == Ground.GRASS_BLOCK;
        return switch (column.cover()) {
            case BARE -> grass && growth(column, settings.sproutShortGrass()).roll(random)
                    ? LandChange.SPROUT_SHORT_GRASS
                    : LandChange.NONE;
            case SHORT_GRASS, FERN -> column.roomAbove() && growth(column, settings.growTall()).roll(random)
                    ? LandChange.GROW_TALL
                    : LandChange.NONE;
            case TALL_GRASS -> column.trees().grows()
                            && growth(column, settings.sapling().scaled(column.trees().rate())).roll(random)
                    ? LandChange.SPROUT_SAPLING
                    : LandChange.NONE;
            case FLOWER -> grass && growth(column, settings.spreadFlower()).roll(random)
                    ? LandChange.SPREAD_FLOWER
                    : LandChange.NONE;
            case OTHER -> LandChange.NONE;
        };
    }

    /** Rain gives growth a second roll. */
    private static Chance growth(Column column, Chance chance) {
        return column.raining() ? chance.twice() : chance;
    }
}
