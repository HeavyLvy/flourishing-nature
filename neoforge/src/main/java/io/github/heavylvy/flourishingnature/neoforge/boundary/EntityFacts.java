package io.github.heavylvy.flourishingnature.neoforge.boundary;

import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.herd.SpeciesId;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;

/** Null-free facts about Minecraft entities. */
public final class EntityFacts {
    private EntityFacts() {}

    public static SpeciesId species(Entity entity) {
        return new SpeciesId(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    public static Position position(Entity entity) {
        return new Position(entity.getX(), entity.getY(), entity.getZ());
    }

    public static boolean isLeashed(Entity entity) {
        return entity instanceof Leashable leashable && leashable.isLeashed();
    }

    public static Optional<Component> customName(Entity entity) {
        return Optional.ofNullable(entity.getCustomName());
    }

    /** Removes a custom name. Minecraft represents "no name" as null. */
    public static void clearCustomName(Entity entity) {
        entity.setCustomName(null);
        entity.setCustomNameVisible(false);
    }

    /** The baby about to be born, if breeding was not already cancelled by someone else. */
    public static Optional<AgeableMob> child(BabyEntitySpawnEvent event) {
        return Optional.ofNullable(event.getChild());
    }
}
