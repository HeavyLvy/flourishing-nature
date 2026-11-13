package io.github.heavylvy.flourishingnature.neoforge.herd;

import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.wolf.Wolf;

/** How fast vanilla's animal hunters run when chasing prey. */
final class Hunters {
    private Hunters() {}

    /**
     * Movement speed times the speed multiplier of the hunter's attack goal in vanilla 26.3:
     * wolves {@code MeleeAttackGoal(1.0)}, foxes {@code FoxMeleeAttackGoal(1.2)}, ocelots and cats
     * {@code OcelotAttackGoal} (about 1.0).
     */
    static Optional<Double> chaseSpeed(Entity hunter) {
        if (!(hunter instanceof LivingEntity living)) {
            return Optional.empty();
        }
        double base = living.getAttributeValue(Attributes.MOVEMENT_SPEED);
        return switch (hunter) {
            case Wolf wolf -> Optional.of(base * 1.0);
            case Fox fox -> Optional.of(base * 1.2);
            case Ocelot ocelot -> Optional.of(base * 1.0);
            case Cat cat -> Optional.of(base * 1.0);
            default -> Optional.empty();
        };
    }
}
