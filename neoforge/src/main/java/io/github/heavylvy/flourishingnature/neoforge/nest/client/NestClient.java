package io.github.heavylvy.flourishingnature.neoforge.nest.client;

import io.github.heavylvy.flourishingnature.core.ModIdentity;
import io.github.heavylvy.flourishingnature.neoforge.boundary.ClientRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/** Client wiring: chickens show the nest material they carry, the way foxes hold items. */
public final class NestClient {
    private NestClient() {}

    static final ContextKey<ItemStackRenderState> BEAK_ITEM =
            new ContextKey<>(Identifier.fromNamespaceAndPath(ModIdentity.MOD_ID, "beak_item"));

    public static void register(IEventBus modBus) {
        modBus.addListener(NestClient::onRenderStateModifiers);
        modBus.addListener(NestClient::onAddLayers);
    }

    private static void onRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.<Chicken, ChickenRenderState>registerEntityModifier(ChickenRenderer.class, NestClient::extract);
    }

    private static void extract(Chicken chicken, ChickenRenderState state) {
        ItemStack held = chicken.getMainHandItem();
        if (held.isEmpty() || chicken.isBaby()) {
            return;
        }
        ItemStackRenderState item = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForLiving(item, held, ItemDisplayContext.GROUND, chicken);
        state.setRenderData(BEAK_ITEM, item);
    }

    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        ClientRenderers.chicken(event).ifPresent(renderer -> renderer.addLayer(new BeakItemLayer(renderer)));
    }
}
