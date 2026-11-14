package io.github.heavylvy.flourishingnature.neoforge.predator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.predator.Dispersal;
import io.github.heavylvy.flourishingnature.core.predator.PackMemory;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Per-wolf saved data for pack life. */
final class PredatorAttachments {
    private PredatorAttachments() {}

    static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModIdentity.MOD_ID);

    private static final MapCodec<PackMemory> MEMORY_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.LONG.fieldOf("last_meal").forGetter(PackMemory::lastMeal),
                    Codec.LONG.fieldOf("last_bred").forGetter(PackMemory::lastBred))
            .apply(i, PackMemory::new));

    private static final MapCodec<Dispersal> DISPERSAL_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.DOUBLE.fieldOf("x").forGetter(d -> d.target().x()),
                    Codec.DOUBLE.fieldOf("y").forGetter(d -> d.target().y()),
                    Codec.DOUBLE.fieldOf("z").forGetter(d -> d.target().z()),
                    Codec.LONG.fieldOf("until").forGetter(Dispersal::until))
            .apply(i, (x, y, z, until) -> new Dispersal(new Position(x, y, z), until)));

    /** Last kill and last pup. A wolf first seen counts as just fed, so it isn't days behind. */
    static final Supplier<AttachmentType<PackMemory>> MEMORY = REGISTER.register(
            "pack_memory",
            () -> AttachmentType.builder(holder -> PackMemory.fresh(
                            holder instanceof Entity e ? e.level().getGameTime() : 0L))
                    .serialize(MEMORY_CODEC)
                    .build());

    /**
     * Where a wolf from a broken pack is heading. Read it with {@code getExistingData}: only
     * travelling wolves have one. The default exists only because the builder requires one.
     */
    static final Supplier<AttachmentType<Dispersal>> DISPERSAL = REGISTER.register(
            "pack_dispersal",
            () -> AttachmentType.builder(() -> new Dispersal(new Position(0, 0, 0), 0L))
                    .serialize(DISPERSAL_CODEC)
                    .build());
}
