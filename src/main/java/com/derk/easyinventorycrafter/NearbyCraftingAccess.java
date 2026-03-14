package com.derk.easyinventorycrafter;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerContext;

public interface NearbyCraftingAccess {
	ScreenHandlerContext derk$getContext();

	PlayerEntity derk$getPlayer();

	void derk$recordNearbyWithdrawal(Inventory inventory, int sourceSlot, int craftingSlotIndex, ItemStack stack, int count, int baselineCount);

	void derk$cancelNearbyWithdrawals();
}
