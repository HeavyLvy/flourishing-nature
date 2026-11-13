package io.github.heavylvy.flourishingnature.core.land;

/**
 * What one sampled spot looks like: its ground and cover, and the few facts about its
 * surroundings the rules need.
 *
 * @param sunlit enough light for plants at the cover block (raw light 9 or more, like crops)
 * @param baseLit block light 8 or more: a torch-lit base, never changed
 * @param raining rain or snow is falling on this spot
 * @param humid the biome is wet enough for moss to creep
 * @param trees the biome's tree growth rate
 * @param tallGrassBeside tall grass grows right next to this spot
 * @param roomAbove two free blocks above the ground, so a plant can grow tall
 */
public record Column(
        Ground ground,
        Cover cover,
        boolean sunlit,
        boolean baseLit,
        boolean raining,
        boolean humid,
        TreeGrowth trees,
        boolean tallGrassBeside,
        boolean roomAbove) {}
