package io.github.heavylvy.flourishingnature.neoforge.herd;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.core.herd.PanicAlarm;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.UUIDUtil;
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

    private static final MapCodec<HerdId> HERD_ID_CODEC = UUIDUtil.CODEC.xmap(HerdId::new, HerdId::value).fieldOf("id");

    /** Which herd the animal belongs to. Absent until the herd planner first places it. */
    public static final Supplier<AttachmentType<HerdId>> HERD = REGISTER.register(
            "herd",
            () -> AttachmentType.builder(() -> new HerdId(new UUID(0L, 0L))).serialize(HERD_ID_CODEC).build());

    /** The level's herds, rebuilt from their members after a restart, so not saved. */
    static final Supplier<AttachmentType<HerdManager>> MANAGER = REGISTER.register(
            "herd_manager", () -> AttachmentType.builder(HerdManager::new).build());
}
