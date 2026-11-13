package io.github.heavylvy.flourishingnature.neoforge;

import com.mojang.logging.LogUtils;
import io.github.heavylvy.flourishingnature.core.ModIdentity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/** NeoForge entrypoint for Flourishing Nature. */
@Mod(ModIdentity.MOD_ID)
public final class FlourishingNatureNeoForge {
    private static final Logger LOGGER = LogUtils.getLogger();

    public FlourishingNatureNeoForge(IEventBus modEventBus) {
        modEventBus.addListener(FlourishingNatureNeoForge::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Flourishing Nature loaded");
    }
}
