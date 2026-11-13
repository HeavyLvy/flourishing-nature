package io.github.heavylvy.flourishingnature.neoforge.boundary;

import java.util.Optional;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/** Null-free access to who is hunting whom. */
public final class Hunting {
    private Hunting() {}

    /** The entity that dealt the damage (the wolf, not its arrow; wolves have none). */
    public static Optional<Entity> killer(DamageSource source) {
        return Optional.ofNullable(source.getEntity());
    }

    public static Optional<LivingEntity> target(Mob mob) {
        return Optional.ofNullable(mob.getTarget());
    }

    /** Stops the mob going after anything. Minecraft represents "no target" as null. */
    public static void clearTarget(Mob mob) {
        mob.setTarget(null);
    }

    /** A reachable spot up to {@code distance} blocks toward {@code goal}, if one turns up. */
    public static Optional<Vec3> stepToward(PathfinderMob mob, int distance, Vec3 goal) {
        return Optional.ofNullable(DefaultRandomPos.getPosTowards(mob, distance, 7, goal, Math.PI / 2));
    }
}
