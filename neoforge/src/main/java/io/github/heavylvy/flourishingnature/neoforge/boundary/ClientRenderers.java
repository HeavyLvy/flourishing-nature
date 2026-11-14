package io.github.heavylvy.flourishingnature.neoforge.boundary;

import java.util.Optional;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Null-free client rendering lookups. Client only: never load this on a dedicated server. */
public final class ClientRenderers {
    private ClientRenderers() {}

    public static Optional<ChickenRenderer> chicken(EntityRenderersEvent.AddLayers event) {
        EntityRenderer<?, ?> renderer = event.getRenderer(EntityTypes.CHICKEN);
        return renderer instanceof ChickenRenderer chicken ? Optional.of(chicken) : Optional.empty();
    }

    public static <T> Optional<T> renderData(EntityRenderState state, ContextKey<T> key) {
        return Optional.ofNullable(state.getRenderData(key));
    }
}
