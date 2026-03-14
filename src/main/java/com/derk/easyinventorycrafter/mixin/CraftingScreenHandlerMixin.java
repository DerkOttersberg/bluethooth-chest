package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.AbstractCraftingScreenHandlerAccess;
import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.PendingNearbyWithdrawal;
import com.derk.easyinventorycrafter.net.NearbyItemsSync;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerMixin implements NearbyCraftingAccess {
	@Unique
	private final List<PendingNearbyWithdrawal> derk$pendingNearbyWithdrawals = new ArrayList<>();

	@Unique
	private final Map<Integer, Integer> derk$slotBaselineCounts = new HashMap<>();

	@Unique
	private boolean derk$reconcilingNearbyWithdrawals;

	@Unique
	private boolean derk$cancellingNearbyWithdrawals;

	@Shadow
	@Final
	private ScreenHandlerContext context;

	@Shadow
	@Final
	private PlayerEntity player;

	@Shadow
	public abstract List<Slot> getInputSlots();

	@Shadow
	public abstract void onContentChanged(Inventory inventory);

	@Override
	public ScreenHandlerContext derk$getContext() {
		return this.context;
	}

	@Override
	public PlayerEntity derk$getPlayer() {
		return this.player;
	}

	@Override
	public void derk$recordNearbyWithdrawal(Inventory inventory, int sourceSlot, int craftingSlotIndex, ItemStack stack, int count, int baselineCount) {
		if (count <= 0 || craftingSlotIndex < 0) {
			return;
		}

		this.derk$slotBaselineCounts.putIfAbsent(craftingSlotIndex, baselineCount);
		this.derk$pendingNearbyWithdrawals.add(new PendingNearbyWithdrawal(
				inventory,
				sourceSlot,
				craftingSlotIndex,
				stack.copyWithCount(1),
				count
		));
	}

	@Override
	public void derk$cancelNearbyWithdrawals() {
		boolean changed = false;
		this.derk$cancellingNearbyWithdrawals = true;
		try {
			while (true) {
				this.derk$reconcileNearbyWithdrawals();
				if (this.derk$pendingNearbyWithdrawals.isEmpty()) {
					break;
				}

				List<Slot> inputSlots = this.getInputSlots();
				boolean passChanged = false;
				for (Iterator<PendingNearbyWithdrawal> iterator = this.derk$pendingNearbyWithdrawals.iterator(); iterator.hasNext(); ) {
					PendingNearbyWithdrawal withdrawal = iterator.next();
					if (withdrawal.craftingSlotIndex() < 0 || withdrawal.craftingSlotIndex() >= inputSlots.size()) {
						iterator.remove();
						continue;
					}

					Slot slot = inputSlots.get(withdrawal.craftingSlotIndex());
					ItemStack slotStack = slot.getStack();
					if (slotStack.isEmpty() || !ItemStack.areItemsAndComponentsEqual(slotStack, withdrawal.templateStack())) {
						iterator.remove();
						continue;
					}

					int removableCount = Math.min(withdrawal.remainingCount(), slotStack.getCount());
					if (removableCount <= 0) {
						iterator.remove();
						continue;
					}

					ItemStack toReturn = withdrawal.templateStack().copyWithCount(removableCount);
					int insertedCount = this.derk$insertBackIntoInventory(withdrawal.sourceInventory(), withdrawal.sourceSlot(), toReturn);
					if (insertedCount <= 0) {
						continue;
					}

					if (insertedCount == slotStack.getCount()) {
						slot.setStackNoCallbacks(ItemStack.EMPTY);
					} else {
						slotStack.decrement(insertedCount);
						slot.markDirty();
					}

					withdrawal.setRemainingCount(withdrawal.remainingCount() - insertedCount);
					withdrawal.sourceInventory().markDirty();
					passChanged = true;
					changed = true;
					if (withdrawal.remainingCount() <= 0) {
						iterator.remove();
					}
				}

				this.derk$cleanupSlotBaselines();
				if (!passChanged) {
					break;
				}
			}
		} finally {
			this.derk$cancellingNearbyWithdrawals = false;
		}

		if (changed) {
			this.derk$refreshAfterNearbyTransfer();
		}
	}

	@Inject(method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/screen/ScreenHandlerContext;)V", at = @At("TAIL"))
	private void derk$sendInitialNearbyItems(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, CallbackInfo ci) {
		if (this.player instanceof ServerPlayerEntity serverPlayer) {
			NearbyItemsSync.sendNearbyItems(serverPlayer);
		}
	}

	@Inject(method = "onContentChanged", at = @At("TAIL"))
	private void derk$reconcileOnContentChanged(Inventory inventory, CallbackInfo ci) {
		if (!this.derk$cancellingNearbyWithdrawals) {
			this.derk$reconcileNearbyWithdrawals();
		}
	}

	@Inject(method = "onInputSlotFillFinish", at = @At("TAIL"))
	private void derk$refreshAfterFill(ServerWorld world, net.minecraft.recipe.RecipeEntry<net.minecraft.recipe.CraftingRecipe> recipe, CallbackInfo ci) {
		if (this.player instanceof ServerPlayerEntity serverPlayer) {
			NearbyItemsSync.sendNearbyItems(serverPlayer);
		}
	}

	@Inject(method = "onClosed", at = @At("TAIL"))
	private void derk$clearNearbyWithdrawals(PlayerEntity player, CallbackInfo ci) {
		this.derk$pendingNearbyWithdrawals.clear();
		this.derk$slotBaselineCounts.clear();
	}

	@Unique
	private void derk$reconcileNearbyWithdrawals() {
		if (this.derk$reconcilingNearbyWithdrawals || this.derk$pendingNearbyWithdrawals.isEmpty()) {
			return;
		}

		this.derk$reconcilingNearbyWithdrawals = true;
		try {
			List<Slot> inputSlots = this.getInputSlots();
			Map<Integer, Integer> cancelableCounts = new HashMap<>();
			for (Integer slotIndex : this.derk$slotBaselineCounts.keySet()) {
				if (slotIndex < 0 || slotIndex >= inputSlots.size()) {
					continue;
				}

				ItemStack slotStack = inputSlots.get(slotIndex).getStack();
				if (slotStack.isEmpty()) {
					cancelableCounts.put(slotIndex, 0);
					continue;
				}

				int baselineCount = this.derk$slotBaselineCounts.getOrDefault(slotIndex, 0);
				cancelableCounts.put(slotIndex, Math.max(0, slotStack.getCount() - baselineCount));
			}

			for (Iterator<PendingNearbyWithdrawal> iterator = this.derk$pendingNearbyWithdrawals.iterator(); iterator.hasNext(); ) {
				PendingNearbyWithdrawal withdrawal = iterator.next();
				if (withdrawal.craftingSlotIndex() < 0 || withdrawal.craftingSlotIndex() >= inputSlots.size()) {
					iterator.remove();
					continue;
				}

				ItemStack slotStack = inputSlots.get(withdrawal.craftingSlotIndex()).getStack();
				if (slotStack.isEmpty() || !ItemStack.areItemsAndComponentsEqual(slotStack, withdrawal.templateStack())) {
					iterator.remove();
					continue;
				}

				int availableForCancel = cancelableCounts.getOrDefault(withdrawal.craftingSlotIndex(), 0);
				if (availableForCancel <= 0) {
					iterator.remove();
					continue;
				}

				withdrawal.setRemainingCount(Math.min(withdrawal.remainingCount(), availableForCancel));
				cancelableCounts.put(withdrawal.craftingSlotIndex(), availableForCancel - withdrawal.remainingCount());
				if (withdrawal.remainingCount() <= 0) {
					iterator.remove();
				}
			}

			this.derk$cleanupSlotBaselines();
		} finally {
			this.derk$reconcilingNearbyWithdrawals = false;
		}
	}

	@Unique
	private int derk$insertBackIntoInventory(Inventory inventory, int preferredSlot, ItemStack stack) {
		int inserted = 0;
		inserted += this.derk$tryInsertIntoSlot(inventory, preferredSlot, stack);
		for (int i = 0; i < inventory.size() && !stack.isEmpty(); i++) {
			if (i == preferredSlot) {
				continue;
			}
			inserted += this.derk$tryInsertIntoSlot(inventory, i, stack);
		}
		return inserted;
	}

	@Unique
	private int derk$tryInsertIntoSlot(Inventory inventory, int slotIndex, ItemStack stack) {
		if (stack.isEmpty() || slotIndex < 0 || slotIndex >= inventory.size() || !inventory.isValid(slotIndex, stack)) {
			return 0;
		}

		ItemStack targetStack = inventory.getStack(slotIndex);
		if (!targetStack.isEmpty() && !ItemStack.areItemsAndComponentsEqual(targetStack, stack)) {
			return 0;
		}

		int maxCount = Math.min(stack.getMaxCount(), inventory.getMaxCount(stack));
		if (maxCount <= 0) {
			return 0;
		}

		if (targetStack.isEmpty()) {
			int inserted = Math.min(stack.getCount(), maxCount);
			inventory.setStack(slotIndex, stack.copyWithCount(inserted));
			stack.decrement(inserted);
			return inserted;
		}

		int inserted = Math.min(stack.getCount(), maxCount - targetStack.getCount());
		if (inserted <= 0) {
			return 0;
		}

		targetStack.increment(inserted);
		stack.decrement(inserted);
		return inserted;
	}

	@Unique
	private void derk$cleanupSlotBaselines() {
		this.derk$slotBaselineCounts.entrySet().removeIf(entry -> this.derk$pendingNearbyWithdrawals.stream().noneMatch(withdrawal -> withdrawal.craftingSlotIndex() == entry.getKey()));
	}

	@Unique
	private void derk$refreshAfterNearbyTransfer() {
		Inventory craftingInventory = ((AbstractCraftingScreenHandlerAccess)this).derk$getCraftingInventory();
		craftingInventory.markDirty();
		this.onContentChanged(craftingInventory);
		((CraftingScreenHandler)(Object)this).sendContentUpdates();
		if (this.player instanceof ServerPlayerEntity serverPlayer) {
			NearbyItemsSync.sendNearbyItems(serverPlayer);
		}
	}

}
