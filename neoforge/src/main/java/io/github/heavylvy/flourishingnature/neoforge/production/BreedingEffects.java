package io.github.heavylvy.flourishingnature.neoforge.production;

import io.github.heavylvy.flourishingnature.core.production.ProductionRules;
import io.github.heavylvy.flourishingnature.core.wellbeing.Mood;
import io.github.heavylvy.flourishingnature.core.wellbeing.Wellbeing;
import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingAttachments;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Breeding gated by {@link ProductionRules#canBreed}, and the thriving bonuses: a 25% shorter
 * cooldown and babies that grow up 25% faster.
 *
 * <p>Hand-feeding a hungry animal still feeds it ({@code FullnessApi} and wellbeing listen at
 * normal priority): the decision is taken before that bite ({@link #beforeFeeding}), and the love
 * mode is replaced by a plain meal after everyone else has seen the event ({@link #afterFeeding}).
 */
public final class BreedingEffects {
    private BreedingEffects() {}

    /** Highest priority: decide from the animal's hunger before this bite fills it. */
    static void beforeFeeding(PlayerInteractEvent.EntityInteract event) {
        if (!ProductionConfig.breeding() || event.getLevel().isClientSide() || !(event.getTarget() instanceof Animal animal)) {
            return;
        }
        // Horses breed through taming and their own feeding code; the birth gate below covers them.
        if (animal instanceof AbstractHorse || !AnimalCondition.tracked(animal) || !animal.isFood(event.getItemStack())) {
            return;
        }
        if (animal.getAge() == 0 && animal.canFallInLove() && !AnimalCondition.of(animal).canBreed()) {
            animal.setData(ProductionAttachments.REFUSE_LOVE_AT, animal.level().getGameTime());
        }
    }

    /** Lowest priority: eat the food without falling in love. */
    static void afterFeeding(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Animal animal) || event.getLevel().isClientSide()) {
            return;
        }
        boolean refuse = animal.getExistingData(ProductionAttachments.REFUSE_LOVE_AT)
                .filter(tick -> tick == animal.level().getGameTime())
                .isPresent();
        if (!refuse) {
            return;
        }
        animal.removeData(ProductionAttachments.REFUSE_LOVE_AT);
        Player player = event.getEntity();
        event.getItemStack().consume(1, player);
        animal.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
    }

    /**
     * Last word on a birth: cancelled if either parent cannot breed (love mode started before it got
     * hungry, or from another mod); otherwise thriving parents get a shorter cooldown and their baby
     * grows faster. Vanilla sets both ages right after this event, so the change waits a tick.
     */
    static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!ProductionConfig.breeding()
                || !(event.getParentA() instanceof Animal a) || !(event.getParentB() instanceof Animal b)
                || !AnimalCondition.tracked(a)) {
            return;
        }
        if (!AnimalCondition.of(a).canBreed() || !AnimalCondition.of(b).canBreed()) {
            event.setCanceled(true);
            return;
        }
        a.setData(ProductionAttachments.AGE_SCALE, ProductionRules.breedingCooldownFactor(mood(a)));
        b.setData(ProductionAttachments.AGE_SCALE, ProductionRules.breedingCooldownFactor(mood(b)));
        Mood parents = new Wellbeing((wellbeing(a) + wellbeing(b)) / 2.0).mood();
        EntityFacts.child(event).ifPresent(child ->
                child.setData(ProductionAttachments.AGE_SCALE, 1.0 / ProductionRules.babyGrowthSpeed(parents)));
    }

    /** Applies a pending age factor once: a cooldown (positive age) or growth time (negative age). */
    static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AgeableMob mob) || mob.level().isClientSide()) {
            return;
        }
        mob.getExistingData(ProductionAttachments.AGE_SCALE).ifPresent(scale -> {
            mob.removeData(ProductionAttachments.AGE_SCALE);
            mob.setAge((int) Math.round(mob.getAge() * scale));
        });
    }

    private static Mood mood(Animal animal) {
        return AnimalCondition.of(animal).mood();
    }

    /** Not yet checked counts as 50, plain content. */
    private static double wellbeing(Animal animal) {
        return animal.getExistingData(WellbeingAttachments.STATE).map(s -> s.wellbeing().value()).orElse(50.0);
    }
}
