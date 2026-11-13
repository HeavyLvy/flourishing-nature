package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herbivore.DangerMemory;
import io.github.heavylvy.flourishingnature.core.herbivore.DangerSpot;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Per-animal and per-level data for herbivores. */
public final class HerbivoreAttachments {
    private HerbivoreAttachments() {}

    static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModIdentity.MOD_ID);

    private static final Codec<Position> POSITION = RecordCodecBuilder.create(i -> i.group(
                    Codec.DOUBLE.fieldOf("x").forGetter(Position::x),
                    Codec.DOUBLE.fieldOf("y").forGetter(Position::y),
                    Codec.DOUBLE.fieldOf("z").forGetter(Position::z))
            .apply(i, Position::new));

    private static final Codec<DangerSpot> SPOT = RecordCodecBuilder.create(i -> i.group(
                    POSITION.fieldOf("where").forGetter(DangerSpot::where),
                    Codec.LONG.fieldOf("at").forGetter(DangerSpot::atTick))
            .apply(i, DangerSpot::new));

    private static final MapCodec<DangerMemory> DANGER_CODEC =
            SPOT.listOf().xmap(DangerMemory::new, DangerMemory::spots).fieldOf("spots");

    /** Places this animal was attacked, fading over a day or two. */
    public static final Supplier<AttachmentType<DangerMemory>> DANGER = REGISTER.register(
            "danger_memory", () -> AttachmentType.builder(() -> DangerMemory.EMPTY).serialize(DANGER_CODEC).build());

    /** Game tick this animal last bred on its own. Absent if it never has. */
    public static final Supplier<AttachmentType<Long>> LAST_SELF_BRED = REGISTER.register(
            "last_self_bred", () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.fieldOf("tick")).build());

    /** Per-level food-check bookkeeping for herd homes; rebuilt after a restart, so not saved. */
    static final Supplier<AttachmentType<ForagingHomes>> HOMES = REGISTER.register(
            "foraging_homes", () -> AttachmentType.builder(ForagingHomes::new).build());
}
