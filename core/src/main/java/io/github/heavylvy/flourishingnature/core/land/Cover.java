package io.github.heavylvy.flourishingnature.core.land;

/** What grows on top of the ground block. */
public enum Cover {
    /** Nothing: open air above the ground. */
    BARE,
    SHORT_GRASS,
    FERN,
    TALL_GRASS,
    FLOWER,
    /** Anything else (a block, a sapling, a crop...): the rules leave it alone. */
    OTHER
}
