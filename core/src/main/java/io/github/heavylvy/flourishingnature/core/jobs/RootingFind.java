package io.github.heavylvy.flourishingnature.core.jobs;

import io.github.heavylvy.flourishingnature.core.random.Randomness;

/** What a rooting pig sometimes turns up. */
public enum RootingFind {
    NOTHING,
    CARROT,
    POTATO,
    BEETROOT,
    BROWN_MUSHROOM,
    RED_MUSHROOM;

    private static final RootingFind[] FINDS = {CARROT, POTATO, BEETROOT, BROWN_MUSHROOM, RED_MUSHROOM};

    /** With {@code findChance}, one of the five finds, equally likely; otherwise nothing. */
    public static RootingFind roll(Randomness random, double findChance) {
        if (random.nextDouble() >= findChance) {
            return NOTHING;
        }
        return FINDS[random.nextIntInclusive(0, FINDS.length - 1)];
    }
}
