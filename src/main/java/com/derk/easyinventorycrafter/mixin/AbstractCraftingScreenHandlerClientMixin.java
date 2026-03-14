package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import java.util.List;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.screen.AbstractCraftingScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCraftingScreenHandler.class)
public class AbstractCraftingScreenHandlerClientMixin {
	@Inject(method = "populateRecipeFinder", at = @At("TAIL"))
	private void derk$addNearbyClientItems(RecipeFinder finder, CallbackInfo ci) {
		List<NearbyItemEntry> entries = NearbyItemsClientState.getEntries();
		for (NearbyItemEntry entry : entries) {
			if (entry.stack().isEmpty() || entry.count() <= 0) {
				continue;
			}

			int remaining = entry.count();
			int maxStackSize = entry.stack().getMaxCount();
			while (remaining > 0) {
				int chunkSize = Math.min(remaining, maxStackSize);
				finder.addInput(entry.stack().copyWithCount(chunkSize), chunkSize);
				remaining -= chunkSize;
			}
		}
	}
}