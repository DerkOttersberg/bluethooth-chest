package com.derk.easyinventorycrafter.net;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record NearbyHighlightResponsePacket(List<BlockPos> positions) {
    public static NearbyHighlightResponsePacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BlockPos> positions = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return new NearbyHighlightResponsePacket(positions);
    }

    public static void encode(NearbyHighlightResponsePacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.positions().size());
        for (BlockPos pos : packet.positions()) {
            buf.writeBlockPos(pos);
        }
    }
}
