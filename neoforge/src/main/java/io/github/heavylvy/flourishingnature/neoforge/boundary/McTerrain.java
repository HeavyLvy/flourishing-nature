package io.github.heavylvy.flourishingnature.neoforge.boundary;

import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.herd.Terrain;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Good herd homes: the surface block at x/z, if it is solid, dry ground with open sky above and
 * not far above or below where we asked. Water, lava, caves and unloaded chunks don't count.
 */
public record McTerrain(Level level) implements Terrain {
    private static final int MAX_STEP = 8;

    @Override
    public Optional<Position> pasture(Position near) {
        BlockPos column = BlockPos.containing(near.x(), near.y(), near.z());
        if (!level.isLoaded(column)) {
            return Optional.empty();
        }
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        if (Math.abs(surface - near.y()) > MAX_STEP) {
            return Optional.empty();
        }
        BlockPos feet = new BlockPos(column.getX(), surface, column.getZ());
        BlockState ground = level.getBlockState(feet.below());
        boolean dry = ground.getFluidState().isEmpty() && level.getFluidState(feet).isEmpty();
        boolean solid = ground.isFaceSturdy(level, feet.below(), Direction.UP);
        if (!dry || !solid || !level.canSeeSky(feet)) {
            return Optional.empty();
        }
        return Optional.of(new Position(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5));
    }
}
