package com.derk.easyinventorycrafter.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReturnNearbyItemsPacket() implements CustomPacketPayload {
    public static final Type<ReturnNearbyItemsPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("derk_easy_inventory_crafter", "return_nearby_items"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReturnNearbyItemsPacket> STREAM_CODEC = StreamCodec.unit(new ReturnNearbyItemsPacket());

    public static ReturnNearbyItemsPacket decode(RegistryFriendlyByteBuf buf) {
        return new ReturnNearbyItemsPacket();
    }

    public static void encode(ReturnNearbyItemsPacket packet, RegistryFriendlyByteBuf buf) {
    }

    @Override
    public Type<ReturnNearbyItemsPacket> type() {
        return TYPE;
    }
}
