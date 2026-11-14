package io.github.heavylvy.flourishingnature.neoforge.nest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.heavylvy.flourishingnature.neoforge.boundary.ClientRenderers;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Draws the carried item crosswise in the chicken's beak, following her head. Modelled on the
 * fox's held-item layer; the offsets come from the adult chicken model (head pivot at 0, 15, -4
 * pixels; beak 4 pixels in front of it, 3 above the pivot).
 */
final class BeakItemLayer extends RenderLayer<ChickenRenderState, ChickenModel> {
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float SCALE = 0.5F;

    BeakItemLayer(RenderLayerParent<ChickenRenderState, ChickenModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ChickenRenderState state, float yRot, float xRot) {
        ClientRenderers.renderData(state, NestClient.BEAK_ITEM).ifPresent(item -> {
            poseStack.pushPose();
            poseStack.translate(0.0F, 15.0F * PIXEL, -4.0F * PIXEL);
            poseStack.rotateDegrees(Axis.YP, yRot);
            poseStack.rotateDegrees(Axis.XP, xRot);
            poseStack.translate(0.0F, -3.0F * PIXEL, -5.0F * PIXEL);
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            poseStack.scale(SCALE, SCALE, SCALE);
            item.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        });
    }
}
