package io.github.heavylvy.flourishingnature.neoforge.boundary;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;

/**
 * Vanilla's avoid-entity goal, plus a callback when the animal starts fleeing so it can warn its
 * herd. Lives in the boundary because vanilla keeps the predator in a nullable field.
 */
public final class AlarmingAvoidGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
    private final Consumer<LivingEntity> onFlee;

    public AlarmingAvoidGoal(
            PathfinderMob mob,
            Class<T> predator,
            Predicate<LivingEntity> isDangerous,
            float range,
            double walkSpeed,
            double sprintSpeed,
            Consumer<LivingEntity> onFlee) {
        super(mob, predator, isDangerous, range, walkSpeed, sprintSpeed, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        this.onFlee = onFlee;
    }

    @Override
    public void start() {
        super.start();
        Optional.ofNullable(toAvoid).ifPresent(onFlee);
    }
}
