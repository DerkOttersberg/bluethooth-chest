package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.derk.easyinventorycrafter.net.NearbyItemsPayload;
import com.derk.easyinventorycrafter.net.RequestNearbyItemsPayload;
import com.derk.easyinventorycrafter.net.NearbyHighlightRequestPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;

public final class NearbyItemsClientState {
	private static List<NearbyItemEntry> entries = Collections.emptyList();
	private static List<BlockPos> highlightPositions = Collections.emptyList();
	private static int highlightTicks;
	private static int highlightTotalTicks;

	private NearbyItemsClientState() {
	}

	public static List<NearbyItemEntry> getEntries() {
		return entries;
	}

	public static void clear() {
		entries = Collections.emptyList();
		highlightPositions = Collections.emptyList();
		highlightTicks = 0;
		highlightTotalTicks = 0;
	}

	public static void requestUpdate() {
		if (ClientPlayNetworking.canSend(RequestNearbyItemsPayload.ID)) {
			ClientPlayNetworking.send(new RequestNearbyItemsPayload());
		}
	}

	public static void applyPayload(NearbyItemsPayload payload) {
		entries = payload.entries();
	}

	public static void requestHighlight(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		if (ClientPlayNetworking.canSend(NearbyHighlightRequestPayload.ID)) {
			ClientPlayNetworking.send(new NearbyHighlightRequestPayload(stack.copyWithCount(1)));
		}
	}

	public static void setHighlight(List<BlockPos> positions, int ticks) {
		highlightPositions = positions == null ? Collections.emptyList() : positions;
		highlightTicks = ticks;
		highlightTotalTicks = ticks;
	}

	public static List<BlockPos> getHighlightPositions() {
		return highlightPositions;
	}

	public static boolean hasHighlight() {
		return !highlightPositions.isEmpty() && highlightTicks > 0;
	}

	public static float getHighlightAlpha() {
		if (highlightTotalTicks <= 0) {
			return 1.0f;
		}
		float progress = 1.0f - (highlightTicks / (float)highlightTotalTicks);
		float alpha = (float)Math.sin(Math.PI * progress);
		return Math.max(0.0f, Math.min(1.0f, alpha));
	}

	public static void tickHighlight(MinecraftClient client) {
		if (highlightPositions.isEmpty() || highlightTicks <= 0 || client.world == null) {
			if (highlightTicks <= 0) {
				highlightPositions = Collections.emptyList();
				highlightTotalTicks = 0;
			}
			return;
		}
		highlightTicks--;
		if (highlightTicks <= 0) {
			highlightPositions = Collections.emptyList();
			highlightTotalTicks = 0;
			return;
		}
	}
}
