package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herbivore.Diet;
import io.github.heavylvy.flourishingnature.core.herbivore.Food;
import io.github.heavylvy.flourishingnature.core.herbivore.ForageCandidate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Finds food in the world. Which blocks and items are which food comes from tags,
 * {@code #flourishing_nature:forage/<food>}, so datapacks can add modded plants.
 */
public final class Forage {
    private Forage() {}

    /** Plants are looked for this far above and below the animal's feet. */
    private static final int VERTICAL_REACH = 2;

    private static final Map<Food, TagKey<Block>> BLOCK_TAGS = new EnumMap<>(Food.class);
    private static final Map<Food, TagKey<Item>> ITEM_TAGS = new EnumMap<>(Food.class);

    static {
        for (Food food : Food.values()) {
            Identifier id = Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, "forage/" + food.name().toLowerCase(java.util.Locale.ROOT));
            if (food.isItem()) {
                ITEM_TAGS.put(food, TagKey.create(Registries.ITEM, id));
            } else {
                BLOCK_TAGS.put(food, TagKey.create(Registries.BLOCK, id));
            }
        }
    }

    /** What food this block is, if any. Only ripe crops and the lower half of tall plants count. */
    public static Optional<Food> food(BlockState state) {
        if (state.isAir()) {
            return Optional.empty();
        }
        if (state.hasProperty(DoublePlantBlock.HALF) && state.getValue(DoublePlantBlock.HALF) != DoubleBlockHalf.LOWER) {
            return Optional.empty();
        }
        if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
            return Optional.empty();
        }
        for (Map.Entry<Food, TagKey<Block>> entry : BLOCK_TAGS.entrySet()) {
            if (state.is(entry.getValue())) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /** What food this item is, if any. */
    public static Optional<Food> food(ItemStack stack) {
        for (Map.Entry<Food, TagKey<Item>> entry : ITEM_TAGS.entrySet()) {
            if (stack.is(entry.getValue())) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /** Every plant and dropped food {@code diet} eats within {@code radius} of {@code centre}. */
    static List<ForageTarget> around(Level level, Vec3 centre, Diet diet, int radius) {
        List<ForageTarget> found = new ArrayList<>();
        Set<Food> menu = diet.menu();
        if (menu.stream().anyMatch(f -> !f.isItem())) {
            BlockPos feet = BlockPos.containing(centre);
            for (BlockPos p : BlockPos.betweenClosed(feet.offset(-radius, -VERTICAL_REACH, -radius),
                    feet.offset(radius, VERTICAL_REACH, radius))) {
                BlockState state = level.getBlockState(p);
                Optional<Food> food = food(state).filter(menu::contains);
                if (food.isPresent()) {
                    BlockPos pos = p.immutable();
                    double distance = Vec3.atBottomCenterOf(pos).distanceTo(centre);
                    found.add(new ForageTarget.Plant(pos, food.get(), new ForageCandidate(food.get(), distance)));
                }
            }
        }
        if (menu.stream().anyMatch(Food::isItem)) {
            AABB box = AABB.ofSize(centre, radius * 2.0, VERTICAL_REACH * 2.0 + 1, radius * 2.0);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive)) {
                food(item.getItem()).filter(menu::contains).ifPresent(food -> found.add(new ForageTarget.Dropped(
                        item, food, new ForageCandidate(food, item.position().distanceTo(centre)))));
            }
        }
        return found;
    }

    /**
     * Edible plants growing within {@code radius} of {@code centre}, for the wellbeing food need.
     * Tall plants count once.
     */
    public static int ediblePlants(Level level, Vec3 centre, Diet diet, int radius) {
        Set<Food> menu = diet.menu();
        BlockPos feet = BlockPos.containing(centre);
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(feet.offset(-radius, -1, -radius), feet.offset(radius, 1, radius))) {
            if (food(level.getBlockState(p)).filter(menu::contains).isPresent()) {
                count++;
            }
        }
        return count;
    }

    /** Food for a herd's home: edible plants in the square, tall grass counting double. */
    static int patchFood(Level level, BlockPos centre, Diet diet, int halfWidth) {
        Set<Food> menu = diet.menu();
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-halfWidth, -3, -halfWidth), centre.offset(halfWidth, 3, halfWidth))) {
            Optional<Food> food = food(level.getBlockState(p)).filter(menu::contains);
            if (food.isPresent()) {
                count += food.get() == Food.TALL_GRASS ? 2 : 1;
            }
        }
        return count;
    }
}
