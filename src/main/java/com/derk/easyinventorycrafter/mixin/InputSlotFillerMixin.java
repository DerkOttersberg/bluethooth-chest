package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.WorldPos;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.InputSlotFiller;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AbstractCraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InputSlotFiller.class)
public abstract class InputSlotFillerMixin {
	@Shadow
	private PlayerInventory inventory;

	@Shadow
	private InputSlotFiller.Handler<?> handler;

	@Shadow
	private boolean craftAll;

	@Inject(method = "fillInputSlot", at = @At("HEAD"), cancellable = true)
	private void derk$fillFromNearby(Slot slot, RegistryEntry<Item> item, int count, CallbackInfoReturnable<Integer> cir) {
		int targetCount = count;
		ItemStack slotStack = slot.getStack();
		int availableInPlayer = this.derk$countInPlayerInventory(item, slotStack);
		if (availableInPlayer >= targetCount) {
			return;
		}

		AbstractCraftingScreenHandler screenHandler = this.derk$resolveScreenHandler();
		if (screenHandler == null || !(screenHandler instanceof NearbyCraftingAccess access)) {
			cir.setReturnValue(-1);
			return;
		}

		ScreenHandlerContext context = access.derk$getContext();
		WorldPos worldPos = NearbyInventoryScanner.getWorldPos(context);
		if (worldPos == null) {
			cir.setReturnValue(-1);
			return;
		}

		World world = worldPos.world();
		List<Inventory> inventories = NearbyInventoryScanner.findNearbyInventories(
				world,
				worldPos.pos(),
				NearbyInventoryScanner.DEFAULT_RADIUS
		);

		int availableInNearby = derk$countInInventories(inventories, item, slotStack);
		if (availableInPlayer + availableInNearby < targetCount) {
			cir.setReturnValue(-1);
			return;
		}

		int remaining = targetCount;
		remaining = this.derk$takeFromPlayerInventory(item, slotStack, slot, remaining);
		slotStack = slot.getStack();
		if (remaining <= 0) {
			cir.setReturnValue(0);
			return;
		}
		for (Inventory inv : inventories) {
			for (int i = 0; i < inv.size(); i++) {
				ItemStack stack = inv.getStack(i);
				if (stack.isEmpty()) {
					continue;
				}

				if (!stack.itemMatches(item) || !PlayerInventory.usableWhenFillingSlot(stack)) {
					continue;
				}

				if (!slotStack.isEmpty() && !ItemStack.areItemsAndComponentsEqual(slotStack, stack)) {
					continue;
				}

				int removeCount = Math.min(remaining, stack.getCount());
				ItemStack removed = inv.removeStack(i, removeCount);
				if (removed.isEmpty()) {
					continue;
				}

				if (slotStack.isEmpty()) {
					slot.setStackNoCallbacks(removed);
					slotStack = removed;
				} else {
					slotStack.increment(removed.getCount());
					slot.markDirty();
				}

				inv.markDirty();
				remaining -= removed.getCount();
				if (remaining <= 0) {
					cir.setReturnValue(0);
					return;
				}
			}
		}

		cir.setReturnValue(remaining == targetCount ? -1 : remaining);
	}

	private int derk$countInInventories(List<Inventory> inventories, RegistryEntry<Item> item, ItemStack slotStack) {
		int total = 0;
		for (Inventory inv : inventories) {
			for (int i = 0; i < inv.size(); i++) {
				ItemStack stack = inv.getStack(i);
				if (stack.isEmpty()) {
					continue;
				}
				if (!stack.itemMatches(item) || !PlayerInventory.usableWhenFillingSlot(stack)) {
					continue;
				}
				if (!slotStack.isEmpty() && !ItemStack.areItemsAndComponentsEqual(slotStack, stack)) {
					continue;
				}
				total += stack.getCount();
				if (total >= Integer.MAX_VALUE - 1) {
					return total;
				}
			}
		}
		return total;
	}

	private int derk$countInPlayerInventory(RegistryEntry<Item> item, ItemStack slotStack) {
		int total = 0;
		for (ItemStack stack : this.inventory.getMainStacks()) {
			if (stack.isEmpty()) {
				continue;
			}
			if (!stack.itemMatches(item) || !PlayerInventory.usableWhenFillingSlot(stack)) {
				continue;
			}
			if (!slotStack.isEmpty() && !ItemStack.areItemsAndComponentsEqual(slotStack, stack)) {
				continue;
			}
			total += stack.getCount();
		}
		return total;
	}

	private int derk$takeFromPlayerInventory(RegistryEntry<Item> item, ItemStack slotStack, Slot slot, int remaining) {
		int stillNeeded = remaining;
		for (int i = 0; i < this.inventory.getMainStacks().size() && stillNeeded > 0; i++) {
			ItemStack stack = this.inventory.getStack(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (!stack.itemMatches(item) || !PlayerInventory.usableWhenFillingSlot(stack)) {
				continue;
			}
			if (!slotStack.isEmpty() && !ItemStack.areItemsAndComponentsEqual(slotStack, stack)) {
				continue;
			}

			int removeCount = Math.min(stillNeeded, stack.getCount());
			ItemStack removed = this.inventory.removeStack(i, removeCount);
			if (removed.isEmpty()) {
				continue;
			}

			if (slotStack.isEmpty()) {
				slot.setStackNoCallbacks(removed);
				slotStack = removed;
			} else {
				slotStack.increment(removed.getCount());
				slot.markDirty();
			}

			stillNeeded -= removed.getCount();
		}

		return stillNeeded;
	}

	@Nullable
	private AbstractCraftingScreenHandler derk$resolveScreenHandler() {
		Object handlerObj = this.handler;
		if (handlerObj == null) {
			return null;
		}

		for (Field field : handlerObj.getClass().getDeclaredFields()) {
			if (AbstractCraftingScreenHandler.class.isAssignableFrom(field.getType())) {
				field.setAccessible(true);
				try {
					return (AbstractCraftingScreenHandler)field.get(handlerObj);
				} catch (IllegalAccessException ignored) {
					return null;
				}
			}
		}

		return null;
	}
}
