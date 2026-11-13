package io.github.heavylvy.flourishingnature.neoforge.production;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.production.MilkCharges;
import java.util.function.Supplier;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Per-animal production data. Only milk is saved; the rest lives for a few ticks. */
final class ProductionAttachments {
    private ProductionAttachments() {}

    static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModIdentity.MOD_ID);

    private static final MapCodec<MilkCharges> MILK_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.intRange(0, Integer.MAX_VALUE).fieldOf("charges").forGetter(MilkCharges::charges),
                    Codec.doubleRange(0.0, 0.999_999_999).fieldOf("progress").forGetter(MilkCharges::progress),
                    Codec.LONG.fieldOf("at").forGetter(MilkCharges::atTick))
            .apply(i, MilkCharges::new));

    /** Milk charges plus the tick they were last worked out. A new animal starts full. */
    static final Supplier<AttachmentType<MilkCharges>> MILK = REGISTER.register(
            "milk",
            () -> AttachmentType.builder(holder -> MilkCharges.full(ProductionConfig.milkSettings(),
                            holder instanceof Animal a ? a.level().getGameTime() : 0L))
                    .serialize(MILK_CODEC)
                    .build());

    /** Game tick until which the animal shakes its head after refusing. */
    static final Supplier<AttachmentType<Long>> SHAKE_UNTIL = REGISTER.register(
            "refusal_shake_until", () -> AttachmentType.builder(() -> 0L).build());

    /** Game tick of a hand-feeding that must not start love mode (set early, acted on late). */
    static final Supplier<AttachmentType<Long>> REFUSE_LOVE_AT = REGISTER.register(
            "refuse_love_at", () -> AttachmentType.builder(() -> -1L).build());

    /** A one-off factor to apply to the animal's age on its next tick (cooldown or growth). */
    static final Supplier<AttachmentType<Double>> AGE_SCALE = REGISTER.register(
            "age_scale", () -> AttachmentType.builder(() -> 1.0).build());

    /** Whether the sheep was sheared last tick, to catch wool growing back. */
    static final Supplier<AttachmentType<Boolean>> WAS_SHEARED = REGISTER.register(
            "was_sheared", () -> AttachmentType.builder(() -> false).build());
}
