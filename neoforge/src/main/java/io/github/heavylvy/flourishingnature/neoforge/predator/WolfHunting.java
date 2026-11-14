package io.github.heavylvy.flourishingnature.neoforge.predator;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.core.herd.Position;
import io.github.heavylvy.flourishingnature.core.predator.Diet;
import io.github.heavylvy.flourishingnature.core.predator.Prey;
import io.github.heavylvy.flourishingnature.core.predator.PreyChoice;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Who a wild wolf hunts. Hungry wolves take vanilla wolf prey (tag {@code flourishing_nature:wolf_prey}:
 * sheep, rabbits, foxes) and baby turtles on land; starving wolves also take the herd grazers in
 * {@code flourishing_nature:wolf_prey_when_starving}. Fed and full wolves leave animals alone. Pets,
 * leashed and name-tagged animals are never prey: they are the player's.
 */
final class WolfHunting {
    private WolfHunting() {}

    static final TagKey<EntityType<?>> USUAL_PREY = tag("wolf_prey");
    static final TagKey<EntityType<?>> STARVING_PREY = tag("wolf_prey_when_starving");

    private static TagKey<EntityType<?>> tag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, path));
    }

    static boolean isWild(Mob mob) {
        return mob instanceof Wolf wolf && !wolf.isTame();
    }

    static Diet diet(Animal wolf) {
        return Diet.of(FullnessApi.band(wolf));
    }

    static boolean isPrey(Animal target, Diet diet, ServerLevel level) {
        if (!target.isAlive() || target instanceof Wolf || EntityFacts.isLeashed(target)
                || EntityFacts.customName(target).isPresent()
                || (target instanceof TamableAnimal pet && pet.isTame())
                || (target instanceof AbstractHorse horse && horse.isTamed())) {
            return false;
        }
        boolean usual = target.is(USUAL_PREY) || (target instanceof Turtle && Turtle.BABY_ON_LAND_SELECTOR.test(target, level));
        return switch (diet) {
            case NOT_HUNTING -> false;
            case USUAL_PREY -> usual;
            case ANY_PREY -> usual || target.is(STARVING_PREY);
        };
    }

    /** Prey of {@code diet} within {@code radius} of {@code center}, by entity id. */
    static Map<Integer, Animal> preyNear(ServerLevel level, Position center, double radius, Diet diet) {
        Map<Integer, Animal> found = new HashMap<>();
        AABB box = AABB.ofSize(new Vec3(center.x(), center.y(), center.z()), 2 * radius, 2 * radius, 2 * radius);
        for (Animal animal : level.getEntitiesOfClass(Animal.class, box, a -> isPrey(a, diet, level))) {
            found.put(animal.getId(), animal);
        }
        return found;
    }

    static List<Prey> asPrey(Map<Integer, Animal> animals) {
        List<Prey> prey = new ArrayList<>();
        animals.forEach((id, animal) -> prey.add(new Prey(id, EntityFacts.position(animal), animal.isBaby(),
                Math.max(0.0, Math.min(1.0, animal.getHealth() / animal.getMaxHealth())))));
        return prey;
    }

    /** The target selector: a hungry wild adult wolf picks the weakest nearby prey. */
    static Optional<LivingEntity> chooseTarget(Mob mob) {
        if (!(mob instanceof Wolf wolf) || !isWild(wolf) || wolf.isBaby() || !(wolf.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        Diet diet = diet(wolf);
        if (!diet.hunts()) {
            return Optional.empty();
        }
        double range = wolf.getAttributeValue(Attributes.FOLLOW_RANGE);
        TargetingConditions conditions = TargetingConditions.forCombat().range(range).ignoreLineOfSight();
        Map<Integer, Animal> nearby = preyNear(level, EntityFacts.position(wolf), range, diet);
        nearby.values().removeIf(a -> !conditions.test(level, wolf, a));
        return PreyChoice.pick(EntityFacts.position(wolf), asPrey(nearby), range)
                .flatMap(p -> Optional.ofNullable(nearby.get(p.handle())));
    }
}
