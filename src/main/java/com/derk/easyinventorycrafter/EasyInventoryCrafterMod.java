package com.derk.easyinventorycrafter;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.util.math.BlockPos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.derk.easyinventorycrafter.net.NearbyItemsSync;
import com.derk.easyinventorycrafter.net.NetworkChannels;

public class EasyInventoryCrafterMod implements ModInitializer {
    public static final String MOD_ID = "derk_easy_inventory_crafter";

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(
                NetworkChannels.REQUEST_NEARBY_ITEMS,
                (server, player, handler, buf, responseSender) -> {
                    server.execute(
                            () -> {
                                if (player.currentScreenHandler instanceof CraftingScreenHandler) {
                                    NearbyItemsSync.sendNearbyItems(player);
                                }
                            });
                });

        ServerPlayNetworking.registerGlobalReceiver(
                NetworkChannels.HIGHLIGHT_REQUEST,
                (server, player, handler, buf, responseSender) -> {
                    ItemStack stack = buf.readItemStack();
                    server.execute(
                            () -> {
                                if (!(player.currentScreenHandler
                                        instanceof CraftingScreenHandler)) {
                                    return;
                                }
                                List<BlockPos> positions =
                                        NearbyItemsSync.findHighlightPositions(player, stack);
                                if (positions != null && !positions.isEmpty()) {
                                    NearbyItemsSync.sendHighlightResponse(player, positions);
                                }
                            });
                });
    }
}
