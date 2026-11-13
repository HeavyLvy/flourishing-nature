package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import io.github.heavylvy.flourishingnature.core.herbivore.Food;
import io.github.heavylvy.flourishingnature.core.herbivore.ForageCandidate;
import io.github.heavylvy.flourishingnature.core.herbivore.Grazing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/** Something an animal could walk to and eat: a plant growing in place, or food on the ground. */
public sealed interface ForageTarget extends Grazing.HasCandidate {

    Food food();

    /** Where the animal walks to. */
    Vec3 where();

    /** A plant growing at {@code pos} (the lower half, for tall plants). */
    record Plant(BlockPos pos, Food food, ForageCandidate candidate) implements ForageTarget {
        @Override
        public Vec3 where() {
            return Vec3.atBottomCenterOf(pos);
        }
    }

    /** Food lying on the ground. */
    record Dropped(ItemEntity item, Food food, ForageCandidate candidate) implements ForageTarget {
        @Override
        public Vec3 where() {
            return item.position();
        }
    }
}
