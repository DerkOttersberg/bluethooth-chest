package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class EasyInventoryCrafterClient {
    private EasyInventoryCrafterClient() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        EasyInventoryCrafterConfig.load();
        NeoForge.EVENT_BUS.addListener(EasyInventoryCrafterClient::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        NearbyItemsClientState.tickHighlight(client);

        if (!(client.screen instanceof CraftingScreen) && !(client.screen instanceof InventoryScreen)) {
            NearbyItemsClientState.resetAutoRefreshCounter();
            return;
        }

        NearbyItemsClientState.incrementAutoRefreshCounter();
        if (NearbyItemsClientState.shouldAutoRefresh(EasyInventoryCrafterConfig.getAutoRefreshTicks())) {
            NearbyItemsClientState.requestUpdate();
            NearbyItemsClientState.resetAutoRefreshCounter();
        }
    }
}
