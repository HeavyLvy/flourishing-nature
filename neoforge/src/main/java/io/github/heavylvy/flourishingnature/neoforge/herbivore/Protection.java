package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.neoforged.neoforge.event.EventHooks;

/** Whether an animal may change a block: mob griefing on, and not in a lit base (block light 8+). */
final class Protection {
    private Protection() {}

    static final int BASE_LIGHT = 8;

    static boolean mayEdit(ServerLevel level, Entity animal, BlockPos pos) {
        return level.getBrightness(LightLayer.BLOCK, pos) < BASE_LIGHT && EventHooks.canEntityGrief(level, animal);
    }
}
