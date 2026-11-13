package io.github.heavylvy.flourishingnature.neoforge.herd;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.core.herd.PanicAlarm;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Per-animal saved data for herding. */
public final class HerdAttachments {
    private HerdAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModIdentity.MOD_ID);

    private static final MapCodec<HerdPreference> PREFERENCE_CODEC = Codec.intRange(HerdPreference.MIN, HerdPreference.MAX)
            .xmap(HerdPreference::new, HerdPreference::value)
            .fieldOf("value");

    /**
     * The animal's herd preference. Read it with {@code getExistingData}: an animal without one has
     * not been rolled yet. The default below exists only because the builder requires one.
     */
    public static final Supplier<AttachmentType<HerdPreference>> PREFERENCE = REGISTER.register(
            "herd_preference",
            () -> AttachmentType.builder(() -> new HerdPreference(1)).serialize(PREFERENCE_CODEC).build());

    /**
     * A herd mate's panic, not saved: after a reload the danger is long gone. Read it with
     * {@code getExistingData}; the default is never used.
     */
    public static final Supplier<AttachmentType<PanicAlarm>> ALARM = REGISTER.register(
            "herd_alarm",
            () -> AttachmentType.builder(() -> PanicAlarm.unknownChaser(new Position(0, 0, 0), 0L)).build());
}
