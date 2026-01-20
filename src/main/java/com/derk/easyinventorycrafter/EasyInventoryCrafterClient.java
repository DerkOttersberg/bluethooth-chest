package com.derk.easyinventorycrafter;

import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.derk.easyinventorycrafter.net.NearbyItemsPayload;
import com.derk.easyinventorycrafter.net.NearbyHighlightResponsePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

public class EasyInventoryCrafterClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(NearbyItemsPayload.ID, (payload, context) -> {
			NearbyItemsClientState.applyPayload(payload);
		});

		ClientPlayNetworking.registerGlobalReceiver(NearbyHighlightResponsePayload.ID, (payload, context) -> {
			NearbyItemsClientState.setHighlight(payload.positions(), 100);
		});

		ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
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

		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
			if (!NearbyItemsClientState.hasHighlight()) {
				return;
			}
			var world = MinecraftClient.getInstance().world;
			if (world == null) {
				return;
			}
			int baseColor = ColorHelper.getArgb(255, 255, 215, 0);
			float alpha = NearbyItemsClientState.getHighlightAlpha();
			int strokeColor = ColorHelper.withAlpha(alpha, baseColor);
			float lineWidth = 3.0f;
			Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos();
			try (var ignored = context.worldRenderer().startDrawingGizmos()) {
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
						drawVisibleBoxLines(box, strokeColor, lineWidth, cameraPos);
					}
				}
			}
		});
	}

	private static void drawVisibleBoxLines(Box box, int color, float width, Vec3d cameraPos) {
		Vec3d v000 = new Vec3d(box.minX, box.minY, box.minZ);
		Vec3d v001 = new Vec3d(box.minX, box.minY, box.maxZ);
		Vec3d v010 = new Vec3d(box.minX, box.maxY, box.minZ);
		Vec3d v011 = new Vec3d(box.minX, box.maxY, box.maxZ);
		Vec3d v100 = new Vec3d(box.maxX, box.minY, box.minZ);
		Vec3d v101 = new Vec3d(box.maxX, box.minY, box.maxZ);
		Vec3d v110 = new Vec3d(box.maxX, box.maxY, box.minZ);
		Vec3d v111 = new Vec3d(box.maxX, box.maxY, box.maxZ);
		Vec3d center = box.getCenter();
		Vec3d toCamera = cameraPos.subtract(center);

		if (toCamera.x > 0.0) {
			drawFaceEdges(v000, v001, v011, v010, color, width); // -X face
		} else {
			drawFaceEdges(v100, v101, v111, v110, color, width); // +X face
		}
		if (toCamera.y > 0.0) {
			drawFaceEdges(v000, v100, v101, v001, color, width); // -Y face
		} else {
			drawFaceEdges(v010, v110, v111, v011, color, width); // +Y face
		}
		if (toCamera.z > 0.0) {
			drawFaceEdges(v000, v100, v110, v010, color, width); // -Z face
		} else {
			drawFaceEdges(v001, v101, v111, v011, color, width); // +Z face
		}
	}

	private static void drawFaceEdges(Vec3d a, Vec3d b, Vec3d c, Vec3d d, int color, float width) {
		GizmoDrawing.line(a, b, color, width).ignoreOcclusion();
		GizmoDrawing.line(b, c, color, width).ignoreOcclusion();
		GizmoDrawing.line(c, d, color, width).ignoreOcclusion();
		GizmoDrawing.line(d, a, color, width).ignoreOcclusion();
	}
}
