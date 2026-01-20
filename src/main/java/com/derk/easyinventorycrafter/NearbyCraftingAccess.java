package com.derk.easyinventorycrafter;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandlerContext;

public interface NearbyCraftingAccess {
	ScreenHandlerContext derk$getContext();

	PlayerEntity derk$getPlayer();
}
