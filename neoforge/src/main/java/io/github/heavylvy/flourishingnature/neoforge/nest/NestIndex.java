package io.github.heavylvy.flourishingnature.neoforge.nest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Where the level's loaded nests are, so a hen looking for one to claim doesn't scan thousands of
 * blocks. Each nest re-adds itself every second while it ticks; entries for nests that are gone
 * are dropped when someone looks.
 */
final class NestIndex {
    private final Set<BlockPos> nests = new HashSet<>();

    static NestIndex of(Level level) {
        return level.getData(NestRegistry.INDEX);
    }

    void add(BlockPos pos) {
        nests.add(pos.immutable());
    }

    /** Loaded nests within {@code radius} blocks of {@code center}. */
    List<NestBlockEntity> near(Level level, BlockPos center, int radius) {
        double max = (double) radius * radius;
        nests.removeIf(pos -> level.isLoaded(pos) && level.getBlockEntity(pos, NestRegistry.NEST_ENTITY.get()).isEmpty());
        return nests.stream()
                .filter(pos -> pos.distSqr(center) <= max && level.isLoaded(pos))
                .flatMap(pos -> level.getBlockEntity(pos, NestRegistry.NEST_ENTITY.get()).stream())
                .toList();
    }
}
