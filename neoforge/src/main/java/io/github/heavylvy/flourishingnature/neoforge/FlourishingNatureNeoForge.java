package io.github.heavylvy.flourishingnature.neoforge;

import com.mojang.logging.LogUtils;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.neoforge.fullness.FullnessApi;
import io.github.heavylvy.flourishingnature.neoforge.gametest.HerbivoreGameTests;
import io.github.heavylvy.flourishingnature.neoforge.gametest.HerdGameTests;
import io.github.heavylvy.flourishingnature.neoforge.gametest.PredatorGameTests;
import io.github.heavylvy.flourishingnature.neoforge.gametest.ProductionGameTests;
import io.github.heavylvy.flourishingnature.neoforge.gametest.WeatherGameTests;
import io.github.heavylvy.flourishingnature.neoforge.gametest.WellbeingGameTests;
import io.github.heavylvy.flourishingnature.neoforge.herbivore.HerbivoreConfig;
import io.github.heavylvy.flourishingnature.neoforge.herbivore.HerbivoreEvents;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdEvents;
import io.github.heavylvy.flourishingnature.neoforge.nest.NestEvents;
import io.github.heavylvy.flourishingnature.neoforge.predator.PredatorEvents;
import io.github.heavylvy.flourishingnature.neoforge.production.ProductionEvents;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/** NeoForge entrypoint for Flourishing Nature. */
@Mod(ModIdentity.MOD_ID)
public final class FlourishingNatureNeoForge {
    private static final Logger LOGGER = LogUtils.getLogger();

    public FlourishingNatureNeoForge(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(FlourishingNatureNeoForge::onCommonSetup);
        HerdEvents.register(modEventBus, NeoForge.EVENT_BUS);
        FullnessApi.register(modEventBus, NeoForge.EVENT_BUS);
        WellbeingEvents.register(modEventBus, NeoForge.EVENT_BUS);
        HerbivoreEvents.register(modEventBus, NeoForge.EVENT_BUS);
        HerbivoreConfig.register(container);
        ProductionEvents.register(modEventBus, NeoForge.EVENT_BUS, container);
        PredatorEvents.register(modEventBus, NeoForge.EVENT_BUS, container);
        NestEvents.register(modEventBus, NeoForge.EVENT_BUS, container);
        HerdGameTests.register(modEventBus);
        WellbeingGameTests.register(modEventBus);
        WeatherGameTests.register(modEventBus);
        HerbivoreGameTests.register(modEventBus);
        ProductionGameTests.register(modEventBus);
        PredatorGameTests.register(modEventBus);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Flourishing Nature loaded");
    }
}
