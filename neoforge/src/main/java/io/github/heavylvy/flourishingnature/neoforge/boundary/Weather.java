package io.github.heavylvy.flourishingnature.neoforge.boundary;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/** Rain, snow and cover, read from the world. */
public final class Weather {
    private Weather() {}

    /**
     * Rain or snow is falling in this spot's biome right now, whether or not the spot is under
     * cover. Deserts, savannas and badlands never count: their biomes have no precipitation.
     */
    public static boolean precipitating(Level level, BlockPos pos) {
        return level.isRaining()
                && level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel()) != Biome.Precipitation.NONE;
    }

    /** Something solid or leafy overhead keeps the rain off the entity's head. */
    public static boolean covered(Entity entity) {
        return !entity.level().canSeeSky(BlockPos.containing(entity.getEyePosition()));
    }

    /**
     * The nearest dry, covered spot an animal can stand in within {@code radius}: under leaves, an
     * overhang, a roof, or just inside a cave mouth. Searches rings outward, a few heights each.
     */
    public static Optional<BlockPos> nearestShelter(Entity entity, int radius) {
        return nearestShelter(entity.level(), entity.blockPosition(), radius);
    }

    /** Same search, around any point. Used by herds scouting for cover. */
    public static Optional<BlockPos> nearestShelter(Level level, BlockPos origin, int radius) {
        for (int r = 2; r <= radius; r += 2) {
            for (int step = 0; step < 16; step++) {
                double angle = step * Math.PI / 8;
                int dx = (int) Math.round(Math.cos(angle) * r);
                int dz = (int) Math.round(Math.sin(angle) * r);
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos feet = origin.offset(dx, dy, dz);
                    if (standableAndCovered(level, feet)) {
                        return Optional.of(feet);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean standableAndCovered(Level level, BlockPos feet) {
        if (!level.isLoaded(feet)) {
            return false;
        }
        BlockState ground = level.getBlockState(feet.below());
        return ground.isFaceSturdy(level, feet.below(), Direction.UP)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.getFluidState(feet).isEmpty()
                && !level.canSeeSky(feet.above());
    }
}
