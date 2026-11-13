package io.github.heavylvy.flourishingnature.neoforge.boundary;

import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.herd.Terrain;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Good pasture that {@code walker} can actually walk to, so a herd never sets off for a spot on
 * the far side of a fence, cliff or river. Costs a pathfinding call, so use it sparingly.
 */
public record ReachableTerrain(Mob walker) implements Terrain {
    @Override
    public Optional<Position> pasture(Position near) {
        return new McTerrain(walker.level()).pasture(near).filter(spot -> {
            BlockPos target = BlockPos.containing(spot.x(), spot.y(), spot.z());
            return Optional.ofNullable(walker.getNavigation().createPath(target, 1)).filter(Path::canReach).isPresent();
        });
    }
}
