package io.github.heavylvy.flourishingnature.core.herbivore;

/** Kinds of food an animal can find lying around or growing in the world. */
public enum Food {
    /** Short grass. */
    SHORT_GRASS,
    /** Tall grass (both halves go when eaten). */
    TALL_GRASS,
    /** Fern or large fern. */
    FERN,
    /** Any flower. */
    FLOWER,
    /** A ripe carrot, potato or beetroot crop. */
    ROOT_CROP,
    /** Ripe wheat. */
    GRAIN_CROP,
    /** Seeds lying on the ground. */
    SEED_ITEM,
    /** A carrot, potato or beetroot lying on the ground. */
    ROOT_ITEM;

    /** Whether this is a dropped item rather than a plant growing in place. */
    public boolean isItem() {
        return switch (this) {
            case SEED_ITEM, ROOT_ITEM -> true;
            case SHORT_GRASS, TALL_GRASS, FERN, FLOWER, ROOT_CROP, GRAIN_CROP -> false;
        };
    }
}
