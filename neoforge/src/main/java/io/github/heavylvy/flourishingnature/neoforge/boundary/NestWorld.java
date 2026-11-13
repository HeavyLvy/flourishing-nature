package io.github.heavylvy.flourishingnature.neoforge.boundary;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Null-free lookups the nest code needs from the world. */
public final class NestWorld {
    private NestWorld() {}

    /** Light from torches and lamps at or above this marks a player's base: hands off. */
    private static final int BASE_LIGHT = 8;

    /** The loaded entity with this id, if any. */
    public static Optional<Entity> entity(Level level, UUID id) {
        return Optional.ofNullable(level.getEntity(id));
    }

    /** A new, not yet added chicken. */
    public static Optional<Chicken> newChicken(Level level) {
        return Optional.ofNullable(EntityTypes.CHICKEN.create(level, EntitySpawnReason.BREEDING));
    }

    /** The level a block entity sits in; absent before it is placed. */
    public static Optional<Level> level(BlockEntity blockEntity) {
        return Optional.ofNullable(blockEntity.getLevel());
    }

    /** A sound everyone nearby hears. */
    public static void playSound(Level level, BlockPos pos, SoundEvent sound, SoundSource source) {
        level.playSound(null, pos, sound, source, 1.0F, 1.0F);
    }

    /** The spec's base protection: animals never change blocks in light of 8 or more. */
    public static boolean protectedByLight(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.BLOCK, pos) >= BASE_LIGHT;
    }
}
