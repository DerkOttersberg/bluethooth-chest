package com.derk.easyinventorycrafter.net;

import com.derk.easyinventorycrafter.EasyInventoryCrafterMod;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record NearbyItemsPayload(List<NearbyItemEntry> entries) implements CustomPayload {
	public static final CustomPayload.Id<NearbyItemsPayload> ID = new CustomPayload.Id<>(
			Identifier.of(EasyInventoryCrafterMod.MOD_ID, "nearby_items")
	);
	public static final PacketCodec<RegistryByteBuf, NearbyItemsPayload> CODEC = PacketCodec.of(
			NearbyItemsPayload::encode,
			NearbyItemsPayload::decode
	);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}

	private static NearbyItemsPayload decode(RegistryByteBuf buf) {
		int size = buf.readVarInt();
		List<NearbyItemEntry> entries = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			ItemStack stack = ItemStack.PACKET_CODEC.decode(buf);
			int count = buf.readVarInt();
			entries.add(new NearbyItemEntry(stack, count));
		}
		return new NearbyItemsPayload(entries);
	}

	private static void encode(NearbyItemsPayload payload, RegistryByteBuf buf) {
		buf.writeVarInt(payload.entries.size());
		for (NearbyItemEntry entry : payload.entries) {
			ItemStack.PACKET_CODEC.encode(buf, entry.stack());
			buf.writeVarInt(entry.count());
		}
	}
}
