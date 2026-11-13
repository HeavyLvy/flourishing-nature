package io.github.heavylvy.flourishingnature.neoforge;

import com.mojang.logging.LogUtils;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.neoforge.gametest.HerdGameTests;
import io.github.heavylvy.flourishingnature.neoforge.gametest.WeatherGameTests;
import io.github.heavylvy.flourishingnature.neoforge.gametest.WellbeingGameTests;
import io.github.heavylvy.flourishingnature.neoforge.herd.HerdEvents;
import io.github.heavylvy.flourishingnature.neoforge.wellbeing.WellbeingEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/** NeoForge entrypoint for Flourishing Nature. */
@Mod(ModIdentity.MOD_ID)
public final class FlourishingNatureNeoForge {
    private static final Logger LOGGER = LogUtils.getLogger();

    public FlourishingNatureNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(FlourishingNatureNeoForge::onCommonSetup);
        HerdEvents.register(modEventBus, NeoForge.EVENT_BUS);
        WellbeingEvents.register(modEventBus, NeoForge.EVENT_BUS);
        HerdGameTests.register(modEventBus);
        WellbeingGameTests.register(modEventBus);
        WeatherGameTests.register(modEventBus);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Flourishing Nature loaded");
    }
}
