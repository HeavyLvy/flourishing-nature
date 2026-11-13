package io.github.heavylvy.flourishingnature.core.jobs;

import java.util.List;

/** Where horses paw for water, and how a water hole dries out. */
public final class WaterHoleRules {
    private WaterHoleRules() {}

    /** Stages of a dry spell before a water hole turns back to dirt. Matches the block state. */
    public static final int MAX_DRYNESS = 7;

    /**
     * Low ground: no surrounding ground is lower than here. Flat ground counts; a hilltop or slope
     * doesn't. Neighbours that couldn't be read are left out of the list.
     */
    public static boolean lowGround(int groundY, List<Integer> neighbourGroundYs) {
        return !neighbourGroundYs.isEmpty() && neighbourGroundYs.stream().allMatch(y -> y >= groundY);
    }

    /** One random tick of the hole: rain resets the dry spell, otherwise it may dry one stage. */
    public static int nextDryness(int dryness, boolean raining, boolean step) {
        if (dryness < 0 || dryness > MAX_DRYNESS) {
            throw new IllegalArgumentException("dryness must be 0.." + MAX_DRYNESS + ": " + dryness);
        }
        if (raining) {
            return 0;
        }
        return step ? Math.min(MAX_DRYNESS, dryness + 1) : dryness;
    }

    /** Past the last stage the hole is gone. */
    public static boolean driedUp(int dryness) {
        return dryness >= MAX_DRYNESS;
    }
}
