package com.derk.easyinventorycrafter.net;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NearbyHighlightResponsePacket(List<BlockPos> positions) implements CustomPacketPayload {
    public static final Type<NearbyHighlightResponsePacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("derk_easy_inventory_crafter", "nearby_highlight_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyHighlightResponsePacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> packet.write(buf), NearbyHighlightResponsePacket::decode);

    public static NearbyHighlightResponsePacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BlockPos> positions = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return new NearbyHighlightResponsePacket(positions);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeBlockPos(pos);
        }
    }

    @Override
    public Type<NearbyHighlightResponsePacket> type() {
        return TYPE;
    }
}
