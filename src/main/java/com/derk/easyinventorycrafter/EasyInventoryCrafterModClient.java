package com.derk.easyinventorycrafter;

import com.derk.easyinventorycrafter.client.EasyInventoryCrafterClient;
import com.derk.easyinventorycrafter.client.EasyInventoryCrafterConfigScreen;
import com.derk.easyinventorycrafter.net.EasyInventoryCrafterNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = EasyInventoryCrafterMod.MOD_ID, dist = Dist.CLIENT)
public final class EasyInventoryCrafterModClient {
    public EasyInventoryCrafterModClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(EasyInventoryCrafterClient::onClientSetup);
        EasyInventoryCrafterNetwork.initClient(modBus);
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new EasyInventoryCrafterConfigScreen(parent));
    }
}