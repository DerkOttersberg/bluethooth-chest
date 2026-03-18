package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public final class NearbyHighlightRenderer {
    private static final float HIGHLIGHT_FACE_OFFSET = 0.003f;
    private static final float DISTANCE_LABEL_HEIGHT = 1.02f;

    private NearbyHighlightRenderer() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
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
        int packedColor = FastColor.ARGB32.color(
            Math.max(0, Math.min(255, (int) (alpha * opacity * 255))),
            (highlightColor >> 16) & 0xFF,
            (highlightColor >> 8) & 0xFF,
            highlightColor & 0xFF
        );

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());

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

            double dx = pos.getX() - cameraPos.x;
            double dy = pos.getY() - cameraPos.y;
            double dz = pos.getZ() - cameraPos.z;
            shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                renderFilledBoxFaces(
                    poseStack,
                    consumer,
                    (float) (dx + minX),
                    (float) (dy + minY),
                    (float) (dz + minZ),
                    (float) (dx + maxX),
                    (float) (dy + maxY),
                    (float) (dz + maxZ),
                    packedColor
                )
            );
        }
        bufferSource.endBatch(RenderType.debugQuads());

        if (!EasyInventoryCrafterConfig.isDistanceLabelEnabled() || mc.player == null) {
            return;
        }

        Set<BlockPos> labeledPositions = new HashSet<>();
        for (BlockPos pos : positions) {
            if (!labeledPositions.add(pos.immutable()) || mc.level.isOutsideBuildHeight(pos)) {
                continue;
            }

            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            Vec3 labelAnchor = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos connectedPos = getConnectedChestPos(pos, state);
                if (positions.contains(connectedPos) && mc.level.isLoaded(connectedPos)) {
                    labeledPositions.add(connectedPos.immutable());
                    labelAnchor = new Vec3(
                        (pos.getX() + connectedPos.getX()) / 2.0 + 0.5,
                        Math.max(pos.getY(), connectedPos.getY()),
                        (pos.getZ() + connectedPos.getZ()) / 2.0 + 0.5
                    );
                }
            }

            double dx = labelAnchor.x - cameraPos.x;
            double dy = labelAnchor.y - cameraPos.y;
            double dz = labelAnchor.z - cameraPos.z;
            renderDistanceLabel(mc, bufferSource, poseStack, labelAnchor, dx, dy, dz, alpha);
        }
        bufferSource.endBatch();
    }

    private static void renderFilledBoxFaces(
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

        addQuad(consumer, matrix, minXOffset, minY, minZOffset, maxXOffset, minY, minZOffset, maxXOffset, maxY, minZOffset, minXOffset, maxY, minZOffset, color);
        addQuad(consumer, matrix, maxXOffset, minY, maxZOffset, minXOffset, minY, maxZOffset, minXOffset, maxY, maxZOffset, maxXOffset, maxY, maxZOffset, color);
        addQuad(consumer, matrix, minXOffset, minY, maxZOffset, minXOffset, minY, minZOffset, minXOffset, maxY, minZOffset, minXOffset, maxY, maxZOffset, color);
        addQuad(consumer, matrix, maxXOffset, minY, minZOffset, maxXOffset, minY, maxZOffset, maxXOffset, maxY, maxZOffset, maxXOffset, maxY, minZOffset, color);
        addQuad(consumer, matrix, minX, maxYOffset, minZ, maxX, maxYOffset, minZ, maxX, maxYOffset, maxZ, minX, maxYOffset, maxZ, color);
        addQuad(consumer, matrix, minX, minYOffset, maxZ, maxX, minYOffset, maxZ, maxX, minYOffset, minZ, minX, minYOffset, minZ, color);
    }

    private static void addQuad(
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

    private static void renderDistanceLabel(
        Minecraft mc,
        MultiBufferSource.BufferSource bufferSource,
        PoseStack poseStack,
        Vec3 labelAnchor,
        double dx,
        double dy,
        double dz,
        float alpha
    ) {
        Vec3 eyePos = mc.player.getEyePosition();
        double meters = Math.sqrt(eyePos.distanceToSqr(labelAnchor));
        String text = String.format(Locale.ROOT, "%.1fm", meters);
        Font font = mc.font;
        float textX = -font.width(text) / 2.0f;
        int textAlpha = Math.max(96, Math.min(255, (int) (alpha * 255.0f)));
        int textColor = FastColor.ARGB32.color(textAlpha, 255, 255, 255);
        int backgroundColor = FastColor.ARGB32.color(Math.max(64, textAlpha / 2), 0, 0, 0);
        float yawDegrees = (float) Math.toDegrees(Math.atan2(eyePos.x - labelAnchor.x, eyePos.z - labelAnchor.z)) + 180.0f;

        poseStack.pushPose();
        poseStack.translate(dx, dy + DISTANCE_LABEL_HEIGHT, dz);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDegrees));
        poseStack.scale(-0.025f, -0.025f, 0.025f);
        Matrix4f matrix = poseStack.last().pose();
        font.drawInBatch(
            Component.literal(text).getVisualOrderText(),
            textX,
            0.0f,
            textColor,
            false,
            matrix,
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            backgroundColor,
            15728880
        );
        poseStack.popPose();
    }

    private static BlockPos getConnectedChestPos(BlockPos pos, BlockState state) {
        Direction connectedDirection = ChestBlock.getConnectedDirection(state);
        return pos.relative(connectedDirection);
    }
}