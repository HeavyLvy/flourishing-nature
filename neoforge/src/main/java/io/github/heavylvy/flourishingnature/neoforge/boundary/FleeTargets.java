package io.github.heavylvy.flourishingnature.neoforge.boundary;

import java.util.Optional;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/** Null-free access to who caused damage. */
public final class FleeTargets {
    private FleeTargets() {}

    /** Who caused the damage, if anyone. */
    public static Optional<Entity> attacker(DamageSource source) {
        return Optional.ofNullable(source.getEntity());
    }
}
