package io.github.heavylvy.flourishingnature.neoforge.boundary;

import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Null-free reads from a loot context. */
public final class LootFacts {
    private LootFacts() {}

    /** The entity the loot comes from (the one that died or was sheared). */
    public static Optional<Entity> thisEntity(LootContext context) {
        return Optional.ofNullable(context.getOptional(LootContextParams.THIS_ENTITY));
    }

    /** Death loot carries a damage source; shearing and other interaction loot do not. */
    public static boolean isDeathLoot(LootContext context) {
        return context.hasParameter(LootContextParams.DAMAGE_SOURCE);
    }
}
