package io.github.heavylvy.flourishingnature.neoforge.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * Builds the 28x28 test pen: a grass floor ringed by a 2-high barrier wall, in the corner of a
 * 64x64 test area. Built in code because the gametest framework placed our structure file's
 * blocks as air; the structure only sets size. The empty margin keeps pens of neighbouring tests
 * (the framework packs tests 5 blocks apart) far enough apart that their herds never interact.
 */
final class TestPens {
    private TestPens() {}

    static final int SIZE = 28;

    static void build(GameTestHelper helper) {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                helper.setBlock(x, 0, z, Blocks.GRASS_BLOCK);
                if (x == 0 || z == 0 || x == SIZE - 1 || z == SIZE - 1) {
                    helper.setBlock(x, 1, z, Blocks.BARRIER);
                    helper.setBlock(x, 2, z, Blocks.BARRIER);
                }
            }
        }
    }
}
