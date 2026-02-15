package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.WorldPos;
import java.util.List;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.InputSlotFiller;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InputSlotFiller.class)
public abstract class InputSlotFillerMixin {
	@Shadow
	protected PlayerInventory inventory;

	@Shadow
	protected AbstractRecipeScreenHandler<?> handler;

	@Inject(method = "fillInputSlot", at = @At("HEAD"), cancellable = true)
	private void derk$fillFromNearby(Slot slot, ItemStack itemStack, CallbackInfo ci) {
		// Only intervene if slot is empty and player doesn't have the item
		ItemStack slotStack = slot.getStack();
		if (!slotStack.isEmpty()) {
			return;
		}

		Item targetItem = itemStack.getItem();

		// Check if player inventory has this item
		boolean playerHasItem = false;
		for (int i = 0; i < this.inventory.main.size(); i++) {
			ItemStack invStack = this.inventory.getStack(i);
			if (!invStack.isEmpty() && invStack.getItem() == targetItem) {
				playerHasItem = true;
				break;
			}
		}

		if (playerHasItem) {
			// Let vanilla handle it
			return;
		}

		// Player doesn't have this item, try to pull from nearby inventories
		if (!(this.handler instanceof NearbyCraftingAccess access)) {
			return;
		}

		ScreenHandlerContext context = access.derk$getContext();
		WorldPos worldPos = NearbyInventoryScanner.getWorldPos(context);
		if (worldPos == null) {
			return;
		}

		World world = worldPos.world();
		List<Inventory> inventories = NearbyInventoryScanner.findNearbyInventories(
				world,
				worldPos.pos(),
				NearbyInventoryScanner.DEFAULT_RADIUS
		);

		for (Inventory inv : inventories) {
			for (int i = 0; i < inv.size(); i++) {
				ItemStack stack = inv.getStack(i);
				if (stack.isEmpty() || stack.getItem() != targetItem) {
					continue;
				}

				ItemStack removed = inv.removeStack(i, 1);
				if (removed.isEmpty()) {
					continue;
				}

				slot.setStack(removed);
				inv.markDirty();
				ci.cancel();
				return;
			}
		}
	}
}
