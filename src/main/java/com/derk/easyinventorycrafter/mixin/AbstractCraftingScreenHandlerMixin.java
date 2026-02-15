package com.derk.easyinventorycrafter.mixin;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.WorldPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreenHandler.class)
public class AbstractCraftingScreenHandlerMixin {
    @Inject(method = "populateRecipeFinder", at = @At("TAIL"))
    private void derk$addNearbyItems(RecipeMatcher finder, CallbackInfo ci) {
        if (!((Object) this instanceof NearbyCraftingAccess access)) {
            return;
        }

        ScreenHandlerContext context = access.derk$getContext();
        WorldPos worldPos = NearbyInventoryScanner.getWorldPos(context);
        if (worldPos == null) {
            return;
        }

        List<Storage<ItemVariant>> storages =
                NearbyInventoryScanner.findNearbyStorages(
                        worldPos.world(), worldPos.pos(), NearbyInventoryScanner.DEFAULT_RADIUS);

        for (Storage<ItemVariant> storage : storages) {
            for (StorageView<ItemVariant> view : storage) {
                if (view.isResourceBlank()) {
                    continue;
                }
                long amount = view.getAmount();
                if (amount <= 0) {
                    continue;
                }

                // RecipeMatcher usually handles inputs by adding them to a list.
                // We create a stack with the total amount (clamped to int max)
                // Assuming RecipeMatcher adds the count of the stack.
                ItemStack stack = view.getResource().toStack();
                stack.setCount((int) Math.min(amount, Integer.MAX_VALUE));
                finder.addInput(stack);
            }
        }
    }
}
