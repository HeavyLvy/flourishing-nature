package io.github.heavylvy.flourishingnature.neoforge.nest;

import io.github.heavylvy.flourishingnature.neoforge.nest.client.NestClient;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Wires nests onto vanilla chickens. */
public final class NestEvents {
    private NestEvents() {}

    /** Kept well above zero every tick, so vanilla's lay-an-egg-on-the-ground timer never fires. */
    private static final int VANILLA_EGG_TIMER = 6000;

    public static void register(IEventBus modBus, IEventBus gameBus, ModContainer container) {
        NestRegistry.register(modBus);
        container.registerConfig(ModConfig.Type.SYNCED, NestConfig.SPEC, NestConfig.FILE_NAME);
        modBus.addListener(NestEvents::onCreativeTab);
        gameBus.addListener(NestEvents::onJoin);
        gameBus.addListener(NestEvents::onTick);
        gameBus.addListener(NestEvents::onDeath);
        // Lowest, and registered after production: a birth that production cancels never gets here.
        gameBus.addListener(EventPriority.LOWEST, BabyEntitySpawnEvent.class, NestEvents::onBabySpawn);
        if (FMLEnvironment.getDist().isClient()) {
            NestClient.register(modBus);
        }
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Chicken hen)) {
            return;
        }
        hen.goalSelector.addGoal(LayInNestGoal.PRIORITY, new LayInNestGoal(hen));
        hen.goalSelector.addGoal(BroodGoal.PRIORITY, new BroodGoal(hen));
        hen.goalSelector.addGoal(GatherNestGoal.PRIORITY, new GatherNestGoal(hen));
    }

    private static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Chicken hen) || hen.level().isClientSide()) {
            return;
        }
        hen.eggTime = VANILLA_EGG_TIMER;
        if (hen.tickCount % HenNests.UPDATE_INTERVAL == 0) {
            HenNests.update(hen);
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Chicken hen && !hen.level().isClientSide()) {
            HenNests.onDeath(hen);
        }
    }

    /**
     * Chickens don't give birth: the hen with a nest (or the first parent) carries a fertile egg
     * and lays it at her nest. Cancelling still resets both parents' love and cooldown.
     */
    private static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof Chicken a) || !(event.getParentB() instanceof Chicken b)) {
            return;
        }
        Chicken mother = HenNests.nest(a).isEmpty() && HenNests.nest(b).isPresent() ? b : a;
        HenNests.addFertileEgg(mother);
        event.setCanceled(true);
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(NestRegistry.NEST_ITEM.get());
        }
    }
}
