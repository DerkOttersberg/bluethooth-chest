package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class EasyInventoryCrafterClient {
    private EasyInventoryCrafterClient() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        EasyInventoryCrafterConfig.load();

        TickEvent.ClientTickEvent.Post.BUS.addListener(clientTick -> {
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
        });
    }
}
