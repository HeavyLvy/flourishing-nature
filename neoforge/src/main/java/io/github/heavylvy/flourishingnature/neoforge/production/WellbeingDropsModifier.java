package io.github.heavylvy.flourishingnature.neoforge.production;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.production.DropRules;
import io.github.heavylvy.flourishingnature.core.production.DropStack;
import io.github.heavylvy.flourishingnature.core.production.ProductionRules;
import io.github.heavylvy.flourishingnature.neoforge.boundary.LootFacts;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Scales a tracked animal's loot by its wellbeing and hunger.
 *
 * <ul>
 *   <li>Death loot: {@link ProductionRules#minimumDropsOnly} leaves one item from
 *       {@code #flourishing_nature:minimum_drop} (meat) and nothing else; a positive
 *       {@link ProductionRules#dropRollBonus} rolls the animal's loot table that many extra times; a
 *       negative one takes that many items off every stack (never below one meat).
 *   <li>Shearing loot: wool is capped at {@link ProductionRules#maxWool}.
 * </ul>
 */
public final class WellbeingDropsModifier extends LootModifier {
    public static final MapCodec<WellbeingDropsModifier> CODEC =
            RecordCodecBuilder.mapCodec(i -> codecStart(i).apply(i, WellbeingDropsModifier::new));

    /** What counts as "1 meat": raw and cooked meat by default. */
    public static final TagKey<Item> MINIMUM_DROP =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, "minimum_drop"));

    public WellbeingDropsModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!ProductionConfig.drops()) {
            return loot;
        }
        Optional<Animal> animal = LootFacts.thisEntity(context)
                .filter(Animal.class::isInstance)
                .map(Animal.class::cast)
                .filter(AnimalCondition::tracked);
        if (animal.isEmpty()) {
            return loot;
        }
        AnimalCondition condition = AnimalCondition.of(animal.get());
        if (LootFacts.isDeathLoot(context)) {
            return deathLoot(loot, context, condition);
        }
        if (animal.get() instanceof Sheep) {
            return capWool(loot, ProductionRules.maxWool(condition.hunger(), countWool(loot)));
        }
        return loot;
    }

    private static ObjectArrayList<ItemStack> deathLoot(ObjectArrayList<ItemStack> loot, LootContext context, AnimalCondition condition) {
        if (ProductionRules.minimumDropsOnly(condition.mood(), condition.hunger())) {
            return apply(loot, DropRules.minimum(asDrops(loot)));
        }
        int bonus = ProductionRules.dropRollBonus(condition.mood(), condition.hunger());
        if (bonus < 0) {
            return apply(loot, DropRules.fewerRolls(asDrops(loot), -bonus));
        }
        if (bonus > 0) {
            LootTable table = context.getLevel().getServer().reloadableRegistries()
                    .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, context.getQueriedLootTableId()));
            for (int i = 0; i < bonus; i++) {
                rollRaw(table, context, loot);
            }
        }
        return loot;
    }

    // The raw roll is deprecated only to steer callers to the modified one; calling the modified
    // one from inside a loot modifier would apply every modifier again, including this one.
    @SuppressWarnings("deprecation")
    private static void rollRaw(LootTable table, LootContext context, ObjectArrayList<ItemStack> loot) {
        table.getRandomItemsRaw(context, loot::add);
    }

    private static List<DropStack> asDrops(List<ItemStack> loot) {
        List<DropStack> drops = new ArrayList<>(loot.size());
        for (ItemStack stack : loot) {
            drops.add(new DropStack(stack.is(MINIMUM_DROP), stack.getCount()));
        }
        return drops;
    }

    private static ObjectArrayList<ItemStack> apply(List<ItemStack> loot, List<DropStack> counts) {
        ObjectArrayList<ItemStack> out = new ObjectArrayList<>();
        for (int i = 0; i < loot.size(); i++) {
            int count = counts.get(i).count();
            if (count > 0) {
                out.add(loot.get(i).copyWithCount(count));
            }
        }
        return out;
    }

    private static int countWool(List<ItemStack> loot) {
        int wool = 0;
        for (ItemStack stack : loot) {
            if (stack.is(ItemTags.WOOL)) {
                wool += stack.getCount();
            }
        }
        return wool;
    }

    private static ObjectArrayList<ItemStack> capWool(List<ItemStack> loot, int max) {
        ObjectArrayList<ItemStack> out = new ObjectArrayList<>();
        int left = max;
        for (ItemStack stack : loot) {
            if (!stack.is(ItemTags.WOOL)) {
                out.add(stack);
                continue;
            }
            int keep = Math.min(left, stack.getCount());
            left -= keep;
            if (keep > 0) {
                out.add(stack.copyWithCount(keep));
            }
        }
        return out;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
