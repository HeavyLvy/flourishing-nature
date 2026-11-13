package io.github.heavylvy.flourishingnature.neoforge.herd;

import io.github.heavylvy.flourishingnature.core.herd.ChaseSpeed;
import io.github.heavylvy.flourishingnature.core.herd.HerdingSettings;
import io.github.heavylvy.flourishingnature.neoforge.boundary.AlarmingAvoidGoal;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;

/**
 * Makes prey run from the predators that hunt them in vanilla, before they get bitten. Only fills
 * gaps: vanilla rabbits already flee wolves and foxes already flee wolves and polar bears.
 *
 * <ul>
 *   <li>Sheep flee wild wolves.
 *   <li>Chickens flee foxes and ocelots.
 *   <li>Rabbits flee foxes and stray cats.
 * </ul>
 *
 * <p>Prey run at {@link ChaseSpeed#PREY_SHARE} of the hunter's chase speed, so the hunter slowly
 * gains and a hunt can still succeed. Tamed wolves and cats don't hunt, so they are ignored.
 */
final class Predators {
    private Predators() {}

    static final int PRIORITY = 1;
    private static final float RANGE = 10.0F;
    private static final double MAX_FLEE_MODIFIER = 1.6;

    /** Chase speeds of vanilla hunters at default attributes (see {@link Hunters}). */
    private static final double WOLF_CHASE = 0.30;
    private static final double FOX_CHASE = 0.36;
    private static final double CAT_CHASE = 0.30;

    static void addFleeGoals(Animal prey) {
        switch (prey) {
            case Sheep sheep -> flee(sheep, Wolf.class, WOLF_CHASE, wolf -> wolf instanceof Wolf w && !w.isTame());
            case Chicken chicken -> {
                flee(chicken, Fox.class, FOX_CHASE, fox -> true);
                flee(chicken, Ocelot.class, CAT_CHASE, ocelot -> true);
            }
            case Rabbit rabbit -> {
                flee(rabbit, Fox.class, FOX_CHASE, fox -> true);
                flee(rabbit, Cat.class, CAT_CHASE, cat -> cat instanceof Cat c && !c.isTame());
            }
            default -> {
                // Not hunted by anything in vanilla, or already flees on its own.
            }
        }
    }

    private static <T extends LivingEntity> void flee(
            Animal prey, Class<T> predator, double chaseSpeed, Predicate<LivingEntity> dangerous) {
        double speed = ChaseSpeed.fleeModifier(
                prey.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), chaseSpeed, MAX_FLEE_MODIFIER);
        prey.goalSelector.addGoal(PRIORITY, new AlarmingAvoidGoal<>(
                prey, predator, dangerous, RANGE, speed, speed,
                hunter -> HerdPanic.raise(prey, EntityFacts.position(hunter),
                        Hunters.chaseSpeed(hunter).orElse(chaseSpeed), HerdingSettings.DEFAULT)));
    }
}
