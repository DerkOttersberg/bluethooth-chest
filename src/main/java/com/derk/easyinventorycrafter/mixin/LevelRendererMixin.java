package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
        method = "renderBlockOutline(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;ZLnet/minecraft/client/renderer/state/LevelRenderState;)V",
        at = @At("RETURN")
    )
    private void derk$renderHighlightBoxes(
        MultiBufferSource.BufferSource bufferSource,
        PoseStack poseStack,
        boolean translucent,
        LevelRenderState renderState,
        CallbackInfo ci
    ) {
        if (!translucent) {
            return;
        }
        if (!NearbyItemsClientState.hasHighlight()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        List<BlockPos> positions = NearbyItemsClientState.getHighlightPositions();
        if (positions.isEmpty()) {
            return;
        }

        float alpha = NearbyItemsClientState.getHighlightAlpha();
        int highlightColor = EasyInventoryCrafterConfig.getHighlightColor();
        float opacity = EasyInventoryCrafterConfig.getHighlightOpacity();
        int a = Math.max(0, Math.min(255, (int) (alpha * opacity * 255)));
        int r = (highlightColor >> 16) & 0xFF;
        int g = (highlightColor >> 8) & 0xFF;
        int b = highlightColor & 0xFF;
        int argb = ARGB.color(a, r, g, b);

        Vec3 camPos = renderState.cameraRenderState.pos;
        float lineWidth = mc.getWindow().getAppropriateLineWidth() * 2.5f;

        var consumer = bufferSource.getBuffer(RenderTypes.lines());
        for (BlockPos pos : positions) {
            if (mc.level.isOutsideBuildHeight(pos)) {
                continue;
            }
            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            VoxelShape shape = state.getShape(mc.level, pos, CollisionContext.empty());
            if (shape.isEmpty()) {
                continue;
            }

            double dx = pos.getX() - camPos.x;
            double dy = pos.getY() - camPos.y;
            double dz = pos.getZ() - camPos.z;
            ShapeRenderer.renderShape(poseStack, consumer, shape, dx, dy, dz, argb, lineWidth);
        }
        bufferSource.endBatch(RenderTypes.lines());

        if (EasyInventoryCrafterConfig.isDistanceLabelEnabled() && mc.player != null) {
            for (BlockPos pos : positions) {
                if (mc.level.isOutsideBuildHeight(pos)) {
                    continue;
                }
                BlockState state = mc.level.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }

                double dx = pos.getX() - camPos.x;
                double dy = pos.getY() - camPos.y;
                double dz = pos.getZ() - camPos.z;
                derk$renderDistanceLabel(mc, bufferSource, poseStack, renderState, pos, dx, dy, dz, alpha);
            }
        }
    }

    private static void derk$renderDistanceLabel(
        Minecraft mc,
        MultiBufferSource.BufferSource bufferSource,
        PoseStack poseStack,
        LevelRenderState renderState,
        BlockPos pos,
        double dx,
        double dy,
        double dz,
        float alpha
    ) {
        Vec3 eyePos = mc.player.getEyePosition();
        double meters = Math.sqrt(eyePos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        String text = String.format(Locale.ROOT, "%.1fm", meters);
        Font font = mc.font;
        float textX = -font.width(text) / 2.0f;
        int textAlpha = Math.max(64, Math.min(255, (int) (alpha * 255.0f)));
        int textColor = ARGB.color(textAlpha, 255, 255, 255);
        int outlineColor = ARGB.color(textAlpha, 0, 0, 0);

        poseStack.pushPose();
        poseStack.translate(dx + 0.5, dy + 1.15, dz + 0.5);
        poseStack.mulPose(renderState.cameraRenderState.orientation);
        poseStack.scale(-0.025f, -0.025f, 0.025f);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch8xOutline(Component.literal(text).getVisualOrderText(), textX, 0.0f, textColor, outlineColor, matrix, bufferSource, 15728880);
        poseStack.popPose();
    }
}
