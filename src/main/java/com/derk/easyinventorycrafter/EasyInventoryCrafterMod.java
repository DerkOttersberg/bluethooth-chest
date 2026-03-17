package com.derk.easyinventorycrafter;

import com.derk.easyinventorycrafter.client.EasyInventoryCrafterClient;
import com.derk.easyinventorycrafter.client.EasyInventoryCrafterConfigScreen;
import com.derk.easyinventorycrafter.net.EasyInventoryCrafterNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EasyInventoryCrafterMod.MOD_ID)
public final class EasyInventoryCrafterMod {
    public static final String MOD_ID = "derk_easy_inventory_crafter";

    public EasyInventoryCrafterMod(FMLJavaModLoadingContext context) {
        EasyInventoryCrafterConfig.load();
        EasyInventoryCrafterNetwork.init();
        FMLClientSetupEvent.getBus(context.getModBusGroup()).addListener(EasyInventoryCrafterClient::onClientSetup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                    (mc, parent) -> new EasyInventoryCrafterConfigScreen(parent)
                )
            )
        );
    }
}
