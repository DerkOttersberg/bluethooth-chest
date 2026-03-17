package com.derk.easyinventorycrafter.net;

import net.minecraft.network.FriendlyByteBuf;

public record RequestNearbyItemsPacket() {
    public static RequestNearbyItemsPacket decode(FriendlyByteBuf buf) {
        return new RequestNearbyItemsPacket();
    }

    public static void encode(RequestNearbyItemsPacket packet, FriendlyByteBuf buf) {
    }
}
