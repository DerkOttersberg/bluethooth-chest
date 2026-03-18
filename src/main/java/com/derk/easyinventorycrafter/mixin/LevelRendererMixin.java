package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
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
    private static final float HIGHLIGHT_FACE_OFFSET = 0.003f;
    private static final float DISTANCE_LABEL_HEIGHT = 1.02f;

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

        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.debugQuads());
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
            shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                derk$renderFilledBoxFaces(
                    poseStack,
                    consumer,
                    (float) (dx + minX),
                    (float) (dy + minY),
                    (float) (dz + minZ),
                    (float) (dx + maxX),
                    (float) (dy + maxY),
                    (float) (dz + maxZ),
                    argb
                )
            );
        }
        bufferSource.endBatch(RenderTypes.debugQuads());

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

    private static void derk$renderFilledBoxFaces(
        PoseStack poseStack,
        VertexConsumer consumer,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        int color
    ) {
        Matrix4f matrix = poseStack.last().pose();

        float minXOffset = minX - HIGHLIGHT_FACE_OFFSET;
        float minYOffset = minY - HIGHLIGHT_FACE_OFFSET;
        float minZOffset = minZ - HIGHLIGHT_FACE_OFFSET;
        float maxXOffset = maxX + HIGHLIGHT_FACE_OFFSET;
        float maxYOffset = maxY + HIGHLIGHT_FACE_OFFSET;
        float maxZOffset = maxZ + HIGHLIGHT_FACE_OFFSET;

        derk$addQuad(
            consumer,
            matrix,
            minXOffset,
            minY,
            minZOffset,
            maxXOffset,
            minY,
            minZOffset,
            maxXOffset,
            maxY,
            minZOffset,
            minXOffset,
            maxY,
            minZOffset,
            color
        );
        derk$addQuad(
            consumer,
            matrix,
            maxXOffset,
            minY,
            maxZOffset,
            minXOffset,
            minY,
            maxZOffset,
            minXOffset,
            maxY,
            maxZOffset,
            maxXOffset,
            maxY,
            maxZOffset,
            color
        );
        derk$addQuad(
            consumer,
            matrix,
            minXOffset,
            minY,
            maxZOffset,
            minXOffset,
            minY,
            minZOffset,
            minXOffset,
            maxY,
            minZOffset,
            minXOffset,
            maxY,
            maxZOffset,
            color
        );
        derk$addQuad(
            consumer,
            matrix,
            maxXOffset,
            minY,
            minZOffset,
            maxXOffset,
            minY,
            maxZOffset,
            maxXOffset,
            maxY,
            maxZOffset,
            maxXOffset,
            maxY,
            minZOffset,
            color
        );
        derk$addQuad(
            consumer,
            matrix,
            minX,
            maxYOffset,
            minZ,
            maxX,
            maxYOffset,
            minZ,
            maxX,
            maxYOffset,
            maxZ,
            minX,
            maxYOffset,
            maxZ,
            color
        );
        derk$addQuad(
            consumer,
            matrix,
            minX,
            minYOffset,
            maxZ,
            maxX,
            minYOffset,
            maxZ,
            maxX,
            minYOffset,
            minZ,
            minX,
            minYOffset,
            minZ,
            color
        );
    }

    private static void derk$addQuad(
        VertexConsumer consumer,
        Matrix4f matrix,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float x4,
        float y4,
        float z4,
        int color
    ) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(color);
        consumer.addVertex(matrix, x2, y2, z2).setColor(color);
        consumer.addVertex(matrix, x3, y3, z3).setColor(color);
        consumer.addVertex(matrix, x4, y4, z4).setColor(color);
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
        double labelX = pos.getX() + 0.5;
        double labelZ = pos.getZ() + 0.5;
        float yawDegrees = (float) Math.toDegrees(Math.atan2(eyePos.x - labelX, eyePos.z - labelZ));

        poseStack.pushPose();
        poseStack.translate(dx + 0.5, dy + DISTANCE_LABEL_HEIGHT, dz + 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDegrees));
        poseStack.scale(-0.025f, -0.025f, 0.025f);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch8xOutline(Component.literal(text).getVisualOrderText(), textX, 0.0f, textColor, outlineColor, matrix, bufferSource, 15728880);
        poseStack.popPose();
    }
}
