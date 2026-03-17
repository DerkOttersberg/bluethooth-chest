package com.derk.easyinventorycrafter.net;

import net.minecraft.network.FriendlyByteBuf;

public record ReturnNearbyItemsPacket() {
    public static ReturnNearbyItemsPacket decode(FriendlyByteBuf buf) {
        return new ReturnNearbyItemsPacket();
    }

    public static void encode(ReturnNearbyItemsPacket packet, FriendlyByteBuf buf) {
    }
}
