package io.github.heavylvy.flourishingnature.neoforge.predator;

import io.github.heavylvy.flourishingnature.neoforge.boundary.EntityFacts;
import io.github.heavylvy.flourishingnature.neoforge.boundary.HuntTargetGoal;
import io.github.heavylvy.flourishingnature.neoforge.boundary.Hunting;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdManager;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Wires wild wolf packs into the game. */
public final class PredatorEvents {
    private PredatorEvents() {}

    /** Where vanilla wolves pick prey (target selector 5). */
    private static final int HUNT_PRIORITY = 5;
    /** Vanilla looks for prey about once every 10 ticks; we keep that pace. */
    private static final int HUNT_INTERVAL = 10;

    public static void register(IEventBus modBus, IEventBus gameBus, ModContainer container) {
        PredatorAttachments.REGISTER.register(modBus);
        container.registerConfig(ModConfig.Type.SYNCED, PredatorConfig.SPEC, "flourishing_nature-predators.toml");
        gameBus.addListener(PredatorEvents::onJoin);
        gameBus.addListener(EventPriority.LOWEST, PredatorEvents::onDeath);
        gameBus.addListener(EventPriority.LOWEST, PredatorEvents::onDrops);
        gameBus.addListener(PredatorEvents::onLevelTick);
        HerdManager.addHomeAdjuster(WolfPacks::driftTowardPrey);
    }

    /**
     * Swaps vanilla's always-on prey targeting (sheep, rabbits, foxes, baby turtles) for hunger-driven
     * hunting, and adds the lone-travel goal. Goals are not saved, so this runs on every load.
     */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Wolf wolf)) {
            return;
        }
        if (PredatorConfig.hungerGatesHunting()) {
            wolf.targetSelector.removeAllGoals(goal -> goal instanceof NonTameRandomTargetGoal<?>);
            wolf.targetSelector.addGoal(HUNT_PRIORITY, new HuntTargetGoal(wolf, WolfHunting::chooseTarget, HUNT_INTERVAL));
        }
        wolf.goalSelector.addGoal(DisperseGoal.PRIORITY, new DisperseGoal(wolf));
    }

    /** The wild wolf that killed this animal, if one did. Wolves don't count as prey. */
    private static Optional<Wolf> wildWolfKill(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof Animal) || victim instanceof Wolf) {
            return Optional.empty();
        }
        return Hunting.killer(source)
                .filter(killer -> killer instanceof Wolf wolf && !wolf.isTame())
                .map(Wolf.class::cast);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || !PredatorConfig.killsFeedPack()) {
            return;
        }
        wildWolfKill(event.getEntity(), event.getSource()).ifPresent(wolf ->
                WolfPacks.shareKill(wolf, EntityFacts.position(event.getEntity()), PredatorConfig.settings()));
    }

    /** The pack eats the carcass, so nothing is left on the ground. XP is untouched (only players earn it). */
    private static void onDrops(LivingDropsEvent event) {
        if (PredatorConfig.killsFeedPack() && wildWolfKill(event.getEntity(), event.getSource()).isPresent()) {
            event.setCanceled(true);
        }
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WolfPacks.onLevelTick(level);
        }
    }
}
