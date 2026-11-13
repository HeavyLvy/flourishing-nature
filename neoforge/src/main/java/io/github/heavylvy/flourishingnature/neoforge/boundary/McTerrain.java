package io.github.heavylvy.flourishingnature.neoforge.boundary;

import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.herd.Terrain;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Good herd homes: a spot near the given point, scanning a few blocks up and down, where an
 * animal can stand on solid, dry ground with open sky above. Water, lava, caves, overhangs and
 * unloaded chunks don't count.
 */
public record McTerrain(Level level) implements Terrain {
    private static final int SCAN_UP = 4;
    private static final int SCAN_DOWN = 8;

    @Override
    public Optional<Position> pasture(Position near) {
        BlockPos start = BlockPos.containing(near.x(), near.y(), near.z());
        if (!level.isLoaded(start)) {
            return Optional.empty();
        }
        for (int dy = SCAN_UP; dy >= -SCAN_DOWN; dy--) {
            BlockPos feet = start.offset(0, dy, 0);
            if (standable(feet)) {
                return level.canSeeSky(feet)
                        ? Optional.of(new Position(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5))
                        : Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** Solid dry ground below, room for an animal's body, no water. */
    private boolean standable(BlockPos feet) {
        BlockState ground = level.getBlockState(feet.below());
        // Barriers are invisible walls, not ground an animal would choose to graze on.
        return ground.isFaceSturdy(level, feet.below(), Direction.UP)
                && !ground.is(Blocks.BARRIER)
                && ground.getFluidState().isEmpty()
                && level.getFluidState(feet).isEmpty()
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }
}
