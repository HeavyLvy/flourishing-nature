package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.HerdId;
import io.github.heavylvy.flourishingnature.core.herd.HerdPreference;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingAttachments;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Animal;

/**
 * Optional name tags showing each animal's herd, toggled with {@code /flourishing herd_debug}, for
 * example {@code [herd] #3FA2 5/5 roaming · wellbeing 72 content}. Only touches animals with no
 * name or with a name this class wrote, so player-named pets keep their names.
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

    /** {@code travel} is " roaming", " settled" or empty when the herd has no home yet. */
    static void show(Animal animal, HerdPreference preference, HerdId herd, int size, String travel) {
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
        String text = String.format(Locale.ROOT, "%s#%s %d/%d%s%s",
                PREFIX, herd.shortName(), size, preference.value(), travel, wellbeing);
        animal.setCustomName(Component.literal(text));
        animal.setCustomNameVisible(true);
    }
}
