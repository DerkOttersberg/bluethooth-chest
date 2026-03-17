package com.derk.easyinventorycrafter.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record NearbyHighlightRequestPacket(ItemStack stack) {
    public static NearbyHighlightRequestPacket decode(RegistryFriendlyByteBuf buf) {
        return new NearbyHighlightRequestPacket(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
    }

    public static void encode(NearbyHighlightRequestPacket packet, RegistryFriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.stack());
    }
}
