package com.derk.easyinventorycrafter;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public final class PendingNearbyWithdrawal {
	private final Inventory sourceInventory;
	private final int sourceSlot;
	private final int craftingSlotIndex;
	private final ItemStack templateStack;
	private int remainingCount;

	public PendingNearbyWithdrawal(Inventory sourceInventory, int sourceSlot, int craftingSlotIndex, ItemStack templateStack, int remainingCount) {
		this.sourceInventory = sourceInventory;
		this.sourceSlot = sourceSlot;
		this.craftingSlotIndex = craftingSlotIndex;
		this.templateStack = templateStack;
		this.remainingCount = remainingCount;
	}

	public Inventory sourceInventory() {
		return this.sourceInventory;
	}

	public int sourceSlot() {
		return this.sourceSlot;
	}

	public int craftingSlotIndex() {
		return this.craftingSlotIndex;
	}

	public ItemStack templateStack() {
		return this.templateStack;
	}

	public int remainingCount() {
		return this.remainingCount;
	}

	public void setRemainingCount(int remainingCount) {
		this.remainingCount = remainingCount;
	}
}