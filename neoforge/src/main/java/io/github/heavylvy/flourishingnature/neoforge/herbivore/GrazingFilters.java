package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;

/**
 * Every rule on what grazers may eat. Other features add their own here (for example, cows not
 * eating next to a fresh cow pat) without touching the grazing goal.
 */
public final class GrazingFilters {
    private GrazingFilters() {}

    private static final List<GrazingFilter> FILTERS = new CopyOnWriteArrayList<>();

    public static void register(GrazingFilter filter) {
        FILTERS.add(filter);
    }

    /** Whether every registered filter lets {@code animal} eat {@code target}. */
    public static boolean allows(ServerLevel level, Animal animal, ForageTarget target) {
        for (GrazingFilter filter : FILTERS) {
            if (!filter.allows(level, animal, target)) {
                return false;
            }
        }
        return true;
    }
}
