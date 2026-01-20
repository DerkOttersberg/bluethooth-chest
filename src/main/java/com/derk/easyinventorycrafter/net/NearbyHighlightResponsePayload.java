package com.derk.easyinventorycrafter.net;

import com.derk.easyinventorycrafter.EasyInventoryCrafterMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;

public record NearbyHighlightResponsePayload(List<BlockPos> positions) implements CustomPayload {
	public static final CustomPayload.Id<NearbyHighlightResponsePayload> ID = new CustomPayload.Id<>(
			Identifier.of(EasyInventoryCrafterMod.MOD_ID, "highlight_response")
	);
	public static final PacketCodec<RegistryByteBuf, NearbyHighlightResponsePayload> CODEC = PacketCodec.of(
			NearbyHighlightResponsePayload::encode,
			NearbyHighlightResponsePayload::decode
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}

	private static NearbyHighlightResponsePayload decode(RegistryByteBuf buf) {
		int size = buf.readVarInt();
		List<BlockPos> positions = new ArrayList<>(Math.max(0, size));
		for (int i = 0; i < size; i++) {
			positions.add(BlockPos.PACKET_CODEC.decode(buf));
		}
		return new NearbyHighlightResponsePayload(positions);
	}

	private static void encode(NearbyHighlightResponsePayload payload, RegistryByteBuf buf) {
		List<BlockPos> positions = payload.positions();
		buf.writeVarInt(positions.size());
		for (BlockPos pos : positions) {
			BlockPos.PACKET_CODEC.encode(buf, pos);
		}
	}
}