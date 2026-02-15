package com.derk.easyinventorycrafter;

import com.derk.easyinventorycrafter.net.NearbyItemsSync;
import com.derk.easyinventorycrafter.net.NetworkChannels;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.screen.CraftingScreenHandler;

public class EasyInventoryCrafterMod implements ModInitializer {
	public static final String MOD_ID = "derk_easy_inventory_crafter";

	@Override
	public void onInitialize() {
		ServerPlayNetworking.registerGlobalReceiver(NetworkChannels.REQUEST_NEARBY_ITEMS, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				if (player.currentScreenHandler instanceof CraftingScreenHandler) {
					NearbyItemsSync.sendNearbyItems(player);
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(NetworkChannels.HIGHLIGHT_REQUEST, (server, player, handler, buf, responseSender) -> {
			net.minecraft.item.ItemStack stack = buf.readItemStack();
			server.execute(() -> {
				if (!(player.currentScreenHandler instanceof CraftingScreenHandler)) {
					return;
				}
				java.util.List<net.minecraft.util.math.BlockPos> positions = NearbyItemsSync.findHighlightPositions(
						player,
						stack
				);
				if (positions != null && !positions.isEmpty()) {
					NearbyItemsSync.sendHighlightResponse(player, positions);
				}
			});
		});
	}
}
