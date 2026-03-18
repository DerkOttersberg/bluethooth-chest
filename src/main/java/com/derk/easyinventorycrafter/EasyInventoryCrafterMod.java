package com.derk.easyinventorycrafter;

import com.derk.easyinventorycrafter.net.EasyInventoryCrafterNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(EasyInventoryCrafterMod.MOD_ID)
public final class EasyInventoryCrafterMod {
    public static final String MOD_ID = "derk_easy_inventory_crafter";

    public EasyInventoryCrafterMod(IEventBus modBus) {
        EasyInventoryCrafterConfig.load();
        EasyInventoryCrafterNetwork.init(modBus);
    }
}
