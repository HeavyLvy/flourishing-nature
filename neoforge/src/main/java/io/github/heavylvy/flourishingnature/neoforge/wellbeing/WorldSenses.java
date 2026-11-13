package io.github.heavylvy.flourishingnature.neoforge.wellbeing;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import io.github.heavylvy.flourishingnature.core.wellbeing.BiomeFit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/**
 * Reads the world around an animal for the wellbeing needs. Samples a handful of blocks instead of
 * scanning whole areas, so checks stay cheap.
 */
final class WorldSenses {
    private WorldSenses() {}

    static final int SKY_REACH = 4;
    static final int ROOF_REACH = 8;
    static final int FOOD_REACH = 8;
    static final double CLOSE_QUARTERS = 3.0;

    /** Blocks that count as pasture. Datapacks can extend {@code #flourishing_nature:pasture}. */
    static final TagKey<Block> PASTURE = TagKey.create(Registries.BLOCK, id("pasture"));

    /** Per species: {@code #flourishing_nature:wellbeing/preferred/<species>} and {@code .../tolerated/<species>}. */
    static BiomeFit biomeFit(Animal animal, SpeciesId species) {
        Holder<Biome> biome = animal.level().getBiome(animal.blockPosition());
        String path = species.value().replace(':', '/');
        if (biome.is(TagKey.create(Registries.BIOME, id("wellbeing/preferred/" + path)))) {
            return BiomeFit.PREFERRED;
        }
        if (biome.is(TagKey.create(Registries.BIOME, id("wellbeing/tolerated/" + path)))) {
            return BiomeFit.TOLERATED;
        }
        return BiomeFit.DISLIKED;
    }

    /** Sky visible at the animal's head, or at any of 8 points {@link #SKY_REACH} blocks away. */
    static boolean skyNearby(Animal animal) {
        Level level = animal.level();
        BlockPos head = BlockPos.containing(animal.getEyePosition());
        if (level.canSeeSky(head)) {
            return true;
        }
        for (BlockPos p : ring(head, SKY_REACH)) {
            if (level.canSeeSky(p)) {
                return true;
            }
        }
        return false;
    }

    /** Rain or snow is falling in the animal's biome right now (whether or not it's under a roof). */
    static boolean precipitating(Animal animal) {
        Level level = animal.level();
        BlockPos pos = animal.blockPosition();
        return level.isRaining()
                && level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel()) != Biome.Precipitation.NONE;
    }

    /** Under cover now, or a covered spot within {@link #ROOF_REACH} blocks. */
    static boolean roofNearby(Animal animal) {
        Level level = animal.level();
        BlockPos head = BlockPos.containing(animal.getEyePosition());
        if (!level.canSeeSky(head)) {
            return true;
        }
        for (int r = 2; r <= ROOF_REACH; r += 2) {
            for (BlockPos p : ring(head, r)) {
                if (!level.canSeeSky(p) && level.getBlockState(p).isAir()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Pasture blocks on a 2-block grid within {@link #FOOD_REACH}, at the animal's ground level ±1. */
    static int foodBlocksNearby(Animal animal) {
        Level level = animal.level();
        BlockPos ground = animal.blockPosition().below();
        int count = 0;
        for (int dx = -FOOD_REACH; dx <= FOOD_REACH; dx += 2) {
            for (int dz = -FOOD_REACH; dz <= FOOD_REACH; dz += 2) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos p = ground.offset(dx, dy, dz);
                    if (level.getBlockState(p).is(PASTURE) && level.getBlockState(p.above()).isAir()) {
                        count++;
                        break;
                    }
                }
            }
        }
        return count;
    }

    static int mobsCloseBy(Animal animal) {
        return animal.level().getEntitiesOfClass(
                        Mob.class,
                        animal.getBoundingBox().inflate(CLOSE_QUARTERS),
                        other -> other.getId() != animal.getId() && other.isAlive())
                .size();
    }

    private static BlockPos[] ring(BlockPos centre, int r) {
        return new BlockPos[] {
            centre.offset(r, 0, 0), centre.offset(-r, 0, 0), centre.offset(0, 0, r), centre.offset(0, 0, -r),
            centre.offset(r, 0, r), centre.offset(-r, 0, r), centre.offset(r, 0, -r), centre.offset(-r, 0, -r)
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, path);
    }
}
