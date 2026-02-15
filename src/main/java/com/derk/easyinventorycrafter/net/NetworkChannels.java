package com.derk.easyinventorycrafter.net;

import com.derk.easyinventorycrafter.EasyInventoryCrafterMod;
import net.minecraft.util.Identifier;

public final class NetworkChannels {
	public static final Identifier NEARBY_ITEMS = new Identifier(EasyInventoryCrafterMod.MOD_ID, "nearby_items");
	public static final Identifier REQUEST_NEARBY_ITEMS = new Identifier(EasyInventoryCrafterMod.MOD_ID, "request_nearby_items");
	public static final Identifier HIGHLIGHT_REQUEST = new Identifier(EasyInventoryCrafterMod.MOD_ID, "highlight_request");
	public static final Identifier HIGHLIGHT_RESPONSE = new Identifier(EasyInventoryCrafterMod.MOD_ID, "highlight_response");

	private NetworkChannels() {
	}
}
