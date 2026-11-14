package io.github.heavylvy.flourishingnature.neoforge.nest;

import com.mojang.serialization.Codec;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The nest block, its item and block entity, the hens' saved nest data, and the material tags. */
public final class NestRegistry {
    private NestRegistry() {}

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ModIdentity.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ModIdentity.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModIdentity.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModIdentity.MOD_ID);

    public static final DeferredBlock<NestBlock> NEST = BLOCKS.registerBlock("nest", NestBlock::new, p -> p
            .mapColor(MapColor.COLOR_YELLOW)
            .strength(0.4F)
            .sound(SoundType.GRASS)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.POPPED));

    public static final DeferredItem<BlockItem> NEST_ITEM = ITEMS.registerSimpleBlockItem(NEST);

    public static final Supplier<BlockEntityType<NestBlockEntity>> NEST_ENTITY = BLOCK_ENTITIES.register(
            "nest", () -> new BlockEntityType<>(NestBlockEntity::new, NEST.get()));

    /** The hen's own nest. Absent while she has none. */
    public static final Supplier<AttachmentType<BlockPos>> HEN_NEST = ATTACHMENTS.register(
            "hen_nest", () -> AttachmentType.builder(() -> BlockPos.ZERO).serialize(BlockPos.CODEC.fieldOf("pos")).build());

    /** Progress toward her next egg, see {@code LayProgress}. */
    public static final Supplier<AttachmentType<Double>> LAY_PROGRESS = ATTACHMENTS.register(
            "lay_progress", () -> AttachmentType.builder(() -> 0.0).serialize(Codec.DOUBLE.fieldOf("ticks")).build());

    /** Fertile eggs she carries from breeding, laid at her next visit to her nest. */
    public static final Supplier<AttachmentType<Integer>> FERTILE_EGGS = ATTACHMENTS.register(
            "fertile_eggs", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("count")).build());

    /** Nest material gathered so far, see {@code BuildPoints}. */
    public static final Supplier<AttachmentType<Integer>> BUILD_POINTS = ATTACHMENTS.register(
            "build_points", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT.fieldOf("points")).build());

    /** The level's loaded nests, refreshed by the nests themselves as they tick. Not saved. */
    static final Supplier<AttachmentType<NestIndex>> INDEX = ATTACHMENTS.register(
            "nest_index", () -> AttachmentType.builder(NestIndex::new).build());

    /** Items worth one point of nest material. */
    public static final TagKey<Item> SMALL_MATERIAL = itemTag("nest_material/small");
    /** Items worth two points. */
    public static final TagKey<Item> LARGE_MATERIAL = itemTag("nest_material/large");
    /** Blocks a hen pulls up for one point (short grass). */
    public static final TagKey<Block> SMALL_MATERIAL_BLOCKS = blockTag("nest_material/small");
    /** Blocks a hen pulls up for two points (tall grass, leaf litter). */
    public static final TagKey<Block> LARGE_MATERIAL_BLOCKS = blockTag("nest_material/large");

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ATTACHMENTS.register(modBus);
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, path));
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, path));
    }
}
