package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.HerdDecision;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.core.herd.SocialAssessment;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingAttachments;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Animal;

/**
 * Optional name tags showing each animal's herd state, toggled with {@code /flourishing herd_debug}.
 * Only touches animals with no name or with a name this class wrote, so player-named pets keep
 * their names.
 */
final class HerdDebug {
    private HerdDebug() {}

    private static final String PREFIX = "[herd] ";
    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);

    static boolean toggle() {
        boolean now = !ENABLED.get();
        ENABLED.set(now);
        return now;
    }

    static void show(Animal animal, HerdPreference preference, SocialAssessment assessment) {
        boolean ours = EntityFacts.customName(animal).map(name -> name.getString().startsWith(PREFIX)).orElse(true);
        if (!ours) {
            return;
        }
        if (!ENABLED.get()) {
            if (EntityFacts.customName(animal).isPresent()) {
                EntityFacts.clearCustomName(animal);
            }
            return;
        }
        String wellbeing = animal.getExistingData(WellbeingAttachments.STATE)
                .map(state -> String.format(Locale.ROOT, " · wellbeing %.0f %s",
                        state.wellbeing().value(), state.wellbeing().mood().name().toLowerCase(Locale.ROOT)))
                .orElse("");
        String text = String.format(Locale.ROOT, "%swants %d · has %.1f · %s%s",
                PREFIX, preference.value(), 1.0 + assessment.socialCount(), label(assessment.decision()), wellbeing);
        animal.setCustomName(Component.literal(text));
        animal.setCustomNameVisible(true);
    }

    private static String label(HerdDecision decision) {
        return switch (decision) {
            case HerdDecision.Content content -> "content";
            case HerdDecision.Lonely lonely -> "lonely";
            case HerdDecision.SeekHerd seek -> "seeking";
            case HerdDecision.Disperse disperse -> "crowded";
        };
    }
}
