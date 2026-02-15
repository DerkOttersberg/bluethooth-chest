package com.derk.easyinventorycrafter.net;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.WorldPos;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class NearbyItemsSync {
	private NearbyItemsSync() {
	}

	public static void sendNearbyItems(ServerPlayerEntity player) {
		if (!(player.currentScreenHandler instanceof CraftingScreenHandler)) {
			return;
		}

		ScreenHandlerContext context = getContext(player);
		if (context == null) {
			return;
		}

		WorldPos worldPos = NearbyInventoryScanner.getWorldPos(context);
		if (worldPos == null) {
			return;
		}

		List<NearbyItemEntry> entries = NearbyInventoryScanner.collectItemCounts(
				worldPos.world(),
				worldPos.pos(),
				NearbyInventoryScanner.DEFAULT_RADIUS
		);

		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(entries.size());
		for (NearbyItemEntry entry : entries) {
			buf.writeItemStack(entry.stack());
			buf.writeVarInt(entry.count());
		}
		ServerPlayNetworking.send(player, NetworkChannels.NEARBY_ITEMS, buf);
	}

	public static void sendHighlightResponse(ServerPlayerEntity player, List<BlockPos> positions) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(positions.size());
		for (BlockPos pos : positions) {
			buf.writeBlockPos(pos);
		}
		ServerPlayNetworking.send(player, NetworkChannels.HIGHLIGHT_RESPONSE, buf);
	}

	private static ScreenHandlerContext getContext(ServerPlayerEntity player) {
		if (player.currentScreenHandler instanceof NearbyCraftingAccess access) {
			return access.derk$getContext();
		}

		return null;
	}

	public static List<BlockPos> findHighlightPositions(ServerPlayerEntity player, ItemStack stack) {
		ScreenHandlerContext context = getContext(player);
		if (context == null) {
			return null;
		}
		WorldPos worldPos = NearbyInventoryScanner.getWorldPos(context);
		if (worldPos == null) {
			return null;
		}
		return NearbyInventoryScanner.findInventoryPositionsWithItem(
				worldPos.world(),
				worldPos.pos(),
				NearbyInventoryScanner.DEFAULT_RADIUS,
				stack.getItem()
		);
	}
}
