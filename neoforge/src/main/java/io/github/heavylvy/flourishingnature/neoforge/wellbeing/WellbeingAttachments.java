package io.github.heavylvy.flourishingnature.neoforge.wellbeing;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.wellbeing.Need;
import io.github.heavylvy.flourishingnature.core.wellbeing.NeedScore;
import io.github.heavylvy.flourishingnature.core.wellbeing.NeedScores;
import io.github.heavylvy.flourishingnature.core.wellbeing.RollingMemory;
import io.github.heavylvy.flourishingnature.core.wellbeing.Wellbeing;
import io.github.heavylvy.flourishingnature.core.wellbeing.WellbeingState;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Per-animal saved wellbeing data. Read both with {@code getExistingData}. */
public final class WellbeingAttachments {
    private WellbeingAttachments() {}

    static final DeferredRegister<AttachmentType<?>> REGISTER =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ModIdentity.MOD_ID);

    /** Absent until the animal's first wellbeing check. */
    public static final Supplier<AttachmentType<WellbeingState>> STATE = REGISTER.register(
            "wellbeing",
            () -> AttachmentType.builder(WellbeingAttachments::unused).serialize(WellbeingCodecs.STATE).build());

    /** Game tick of the animal's last meal from a player. Absent if never fed. */
    public static final Supplier<AttachmentType<Long>> LAST_MEAL = REGISTER.register(
            "last_meal",
            () -> AttachmentType.builder(() -> 0L).serialize(WellbeingCodecs.GAME_TICK).build());

    /** The builder needs a default; ours are only ever read with getExistingData. */
    private static WellbeingState unused() {
        Map<Need, NeedScore> scores = new EnumMap<>(Need.class);
        for (Need need : Need.values()) {
            scores.put(need, NeedScore.FULL);
        }
        return new WellbeingState(new Wellbeing(Wellbeing.MAX), new NeedScores(scores), RollingMemory.ALWAYS, RollingMemory.ALWAYS);
    }
}
