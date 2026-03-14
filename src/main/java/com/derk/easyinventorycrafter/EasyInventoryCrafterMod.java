package com.derk.easyinventorycrafter;

import com.derk.easyinventorycrafter.net.NearbyItemsSync;
import com.derk.easyinventorycrafter.net.NearbyItemsPayload;
import com.derk.easyinventorycrafter.net.RequestNearbyItemsPayload;
import com.derk.easyinventorycrafter.net.NearbyHighlightRequestPayload;
import com.derk.easyinventorycrafter.net.NearbyHighlightResponsePayload;
import com.derk.easyinventorycrafter.net.ReturnNearbyItemsPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.screen.CraftingScreenHandler;

public class EasyInventoryCrafterMod implements ModInitializer {
	public static final String MOD_ID = "derk_easy_inventory_crafter";

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(RequestNearbyItemsPayload.ID, RequestNearbyItemsPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(NearbyItemsPayload.ID, NearbyItemsPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(NearbyHighlightRequestPayload.ID, NearbyHighlightRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(NearbyHighlightResponsePayload.ID, NearbyHighlightResponsePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ReturnNearbyItemsPayload.ID, ReturnNearbyItemsPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RequestNearbyItemsPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				if (context.player().currentScreenHandler instanceof CraftingScreenHandler) {
					NearbyItemsSync.sendNearbyItems(context.player());
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(NearbyHighlightRequestPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				if (!(context.player().currentScreenHandler instanceof CraftingScreenHandler)) {
					return;
				}
				java.util.List<net.minecraft.util.math.BlockPos> positions = NearbyItemsSync.findHighlightPositions(
						context.player(),
						payload.stack()
				);
				if (positions != null && !positions.isEmpty()) {
					ServerPlayNetworking.send(context.player(), new NearbyHighlightResponsePayload(positions));
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(ReturnNearbyItemsPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				if (context.player().currentScreenHandler instanceof NearbyCraftingAccess access) {
					access.derk$cancelNearbyWithdrawals();
				}
			});
		});
	}
}
