package io.github.heavylvy.flourishingnature.core.land;

/** One change to the land at a sampled spot. */
public enum LandChange {
    NONE,
    /** Grass block with nothing on it grows short grass (or a fern where ferns grow). */
    SPROUT_SHORT_GRASS,
    /** Short grass or a fern grows into its two-block-tall form. */
    GROW_TALL,
    /** A flower seeds the same flower onto a free grass block nearby. */
    SPREAD_FLOWER,
    /** Ungrazed tall grass gives way to the biome's tree sapling. */
    SPROUT_SAPLING,
    /** Coarse dirt next to tall grass loosens back to dirt, which grass then reclaims. */
    LOOSEN_COARSE_DIRT,
    /** Moss creeps onto a neighbouring stone, cobblestone or gravel block. */
    CREEP_MOSS,
    /** Gravel under a moss carpet slowly becomes dirt. */
    GRAVEL_TO_DIRT
}
