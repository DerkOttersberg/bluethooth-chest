package com.derk.easyinventorycrafter;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.derk.easyinventorycrafter.client.NearbyPanelAccess;
import com.derk.easyinventorycrafter.net.NetworkChannels;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

public class EasyInventoryCrafterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                NetworkChannels.NEARBY_ITEMS,
                (client, handler, buf, responseSender) -> {
                    int size = buf.readVarInt();
                    List<NearbyItemEntry> receivedEntries = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        ItemStack stack = buf.readItemStack();
                        int count = buf.readVarInt();
                        receivedEntries.add(new NearbyItemEntry(stack, count));
                    }
                    client.execute(() -> NearbyItemsClientState.setEntries(receivedEntries));
                });

        ClientPlayNetworking.registerGlobalReceiver(
                NetworkChannels.HIGHLIGHT_RESPONSE,
                (client, handler, buf, responseSender) -> {
                    int size = buf.readVarInt();
                    List<BlockPos> positions = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        positions.add(buf.readBlockPos());
                    }
                    client.execute(() -> NearbyItemsClientState.setHighlight(positions, 100));
                });

        ClientTickEvents.END_CLIENT_TICK.register(
                new ClientTickEvents.EndTick() {
                    private int tickCounter = 0;

                    @Override
                    public void onEndTick(MinecraftClient client) {
                        NearbyItemsClientState.tickHighlight(client);
                        if (!(client.currentScreen instanceof CraftingScreen)) {
                            tickCounter = 0;
                            return;
                        }

                        tickCounter++;
                        if (tickCounter >= 20) {
                            NearbyItemsClientState.requestUpdate();
                            tickCounter = 0;
                        }
                    }
                });

        ScreenEvents.BEFORE_INIT.register(
                (client, screen, width, height) -> {
                    if (!(screen instanceof CraftingScreen)) {
                        return;
                    }
                    ScreenKeyboardEvents.allowKeyPress(screen)
                            .register(
                                    (s, key, scancode, modifiers) -> {
                                        if (s instanceof NearbyPanelAccess access) {
                                            return !access.derk$handleKeyPressed(
                                                    key, scancode, modifiers);
                                        }
                                        return true;
                                    });
                    ScreenMouseEvents.allowMouseClick(screen)
                            .register(
                                    (s, mouseX, mouseY, button) -> {
                                        if (s instanceof NearbyPanelAccess access) {
                                            return !access.derk$handleMouseClick(
                                                    mouseX, mouseY, button);
                                        }
                                        return true;
                                    });
                    ScreenMouseEvents.allowMouseScroll(screen)
                            .register(
                                    (s, mouseX, mouseY, horizontal, vertical) -> {
                                        if (s instanceof NearbyPanelAccess access) {
                                            return !access.derk$handleScroll(
                                                    mouseX, mouseY, vertical);
                                        }
                                        return true;
                                    });
                });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(
                context -> {
                    if (!NearbyItemsClientState.hasHighlight()) {
                        return;
                    }
                    var world = MinecraftClient.getInstance().world;
                    if (world == null) {
                        return;
                    }
                    float alpha = NearbyItemsClientState.getHighlightAlpha();
                    Vec3d cameraPos = context.camera().getPos();

                    MatrixStack matrices = context.matrixStack();
                    matrices.push();
                    matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.disableDepthTest();
                    RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                    RenderSystem.lineWidth(3.0f);

                    Tessellator tessellator = Tessellator.getInstance();
                    BufferBuilder buffer = tessellator.getBuffer();
                    buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

                    Matrix4f matrix = matrices.peek().getPositionMatrix();
                    float r = 1.0f;
                    float g = 215.0f / 255.0f;
                    float b = 0.0f;

                    for (BlockPos pos : NearbyItemsClientState.getHighlightPositions()) {
                        BlockState state = world.getBlockState(pos);
                        if (state.isAir()) {
                            continue;
                        }
                        VoxelShape shape = state.getOutlineShape(world, pos);
                        if (shape.isEmpty()) {
                            continue;
                        }
                        for (Box shapeBox : shape.getBoundingBoxes()) {
                            Box box = shapeBox.offset(pos).expand(0.002);
                            drawVisibleBoxLines(buffer, matrix, box, r, g, b, alpha, cameraPos);
                        }
                    }

                    tessellator.draw();

                    RenderSystem.enableDepthTest();
                    RenderSystem.disableBlend();

                    matrices.pop();
                });
    }

    private static void drawVisibleBoxLines(
            BufferBuilder buffer,
            Matrix4f matrix,
            Box box,
            float r,
            float g,
            float b,
            float a,
            Vec3d cameraPos) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;

        Vec3d center = box.getCenter();
        Vec3d toCamera = cameraPos.subtract(center);

        if (toCamera.x > 0.0) {
            // -X face
            drawFaceEdges(
                    buffer, matrix, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
        } else {
            // +X face
            drawFaceEdges(
                    buffer, matrix, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        }
        if (toCamera.y > 0.0) {
            // -Y face
            drawFaceEdges(
                    buffer, matrix, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
        } else {
            // +Y face
            drawFaceEdges(
                    buffer, matrix, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a);
        }
        if (toCamera.z > 0.0) {
            // -Z face
            drawFaceEdges(
                    buffer, matrix, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a);
        } else {
            // +Z face
            drawFaceEdges(
                    buffer, matrix, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        }
    }

    private static void drawFaceEdges(
            BufferBuilder buffer,
            Matrix4f matrix,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz,
            float r,
            float g,
            float b,
            float a) {
        line(buffer, matrix, ax, ay, az, bx, by, bz, r, g, b, a);
        line(buffer, matrix, bx, by, bz, cx, cy, cz, r, g, b, a);
        line(buffer, matrix, cx, cy, cz, dx, dy, dz, r, g, b, a);
        line(buffer, matrix, dx, dy, dz, ax, ay, az, r, g, b, a);
    }

    private static void line(
            BufferBuilder buffer,
            Matrix4f matrix,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float r,
            float g,
            float b,
            float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
    }
}
