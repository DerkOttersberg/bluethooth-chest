package com.derk.easyinventorycrafter.client;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import com.derk.easyinventorycrafter.net.NetworkChannels;

public final class NearbyItemsClientState {
    private static List<NearbyItemEntry> entries = Collections.emptyList();
    private static List<BlockPos> highlightPositions = Collections.emptyList();
    private static int highlightTicks;
    private static int highlightTotalTicks;

    private NearbyItemsClientState() {}

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
        if (ClientPlayNetworking.canSend(NetworkChannels.REQUEST_NEARBY_ITEMS)) {
            ClientPlayNetworking.send(NetworkChannels.REQUEST_NEARBY_ITEMS, PacketByteBufs.empty());
        }
    }

    public static void setEntries(List<NearbyItemEntry> newEntries) {
        entries = newEntries;
    }

    public static void requestHighlight(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (ClientPlayNetworking.canSend(NetworkChannels.HIGHLIGHT_REQUEST)) {
            PacketByteBuf buf = PacketByteBufs.create();
            ItemStack copy = stack.copy();
            copy.setCount(1);
            buf.writeItemStack(copy);
            ClientPlayNetworking.send(NetworkChannels.HIGHLIGHT_REQUEST, buf);
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
        float progress = 1.0f - (highlightTicks / (float) highlightTotalTicks);
        float alpha = (float) Math.sin(Math.PI * progress);
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
