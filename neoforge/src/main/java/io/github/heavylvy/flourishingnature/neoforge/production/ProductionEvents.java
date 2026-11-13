package io.github.heavylvy.flourishingnature.neoforge.production;

import com.mojang.serialization.MapCodec;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Wires up production: milk charges, wellbeing drops, wool and breeding effects. */
public final class ProductionEvents {
    private ProductionEvents() {}

    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ModIdentity.MOD_ID);

    static {
        LOOT_MODIFIERS.register("wellbeing_drops", () -> WellbeingDropsModifier.CODEC);
    }

    public static void register(IEventBus modBus, IEventBus gameBus, ModContainer container) {
        ProductionConfig.register(container);
        ProductionAttachments.REGISTER.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        gameBus.addListener(Milking::onInteract);
        gameBus.addListener(Milking::onTick);
        gameBus.addListener(EventPriority.HIGHEST, BreedingEffects::beforeFeeding);
        gameBus.addListener(EventPriority.LOWEST, BreedingEffects::afterFeeding);
        gameBus.addListener(EventPriority.LOWEST, BabyEntitySpawnEvent.class, BreedingEffects::onBabySpawn);
        gameBus.addListener(BreedingEffects::onTick);
        gameBus.addListener(WoolGrowth::onTick);
    }
}
