package io.github.heavylvy.flourishingnature.neoforge.boundary;

import net.minecraft.world.entity.animal.Animal;

/** Love mode without a player. */
public final class Love {
    private Love() {}

    /** Puts {@code animal} in love on its own; vanilla marks "no player" with null. */
    public static void startWithoutPlayer(Animal animal) {
        animal.setInLove(null);
    }
}
