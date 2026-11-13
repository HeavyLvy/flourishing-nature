package io.github.heavylvy.flourishingnature.neoforge.herbivore;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;

/**
 * An extra rule on what an animal may eat, such as "never the last flower of its kind nearby".
 * Register with {@link GrazingFilters#register}. Filters run only on the few candidates of one
 * search, so a filter may read the world around the target, but should stay cheap.
 */
@FunctionalInterface
public interface GrazingFilter {
    /** Whether {@code animal} may eat {@code target}. */
    boolean allows(ServerLevel level, Animal animal, ForageTarget target);
}
