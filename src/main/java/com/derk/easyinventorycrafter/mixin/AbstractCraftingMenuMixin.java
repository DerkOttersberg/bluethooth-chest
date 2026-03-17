package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCraftingMenu.class)
public class AbstractCraftingMenuMixin {
    @Inject(method = "fillCraftSlotsStackedContents", at = @At("TAIL"))
    private void derk$addNearbyItems(StackedItemContents contents, CallbackInfo ci) {
        AbstractCraftingMenu menu = (AbstractCraftingMenu) (Object) this;
        NearbyInventoryScanner.WorldPos worldPos = null;

        if (menu instanceof NearbyCraftingAccess access) {
            ContainerLevelAccess levelAccess = access.derk$getAccess();
            worldPos = levelAccess.evaluate((level, pos) -> new NearbyInventoryScanner.WorldPos(level, pos)).orElse(null);
        } else if (menu instanceof InventoryMenu inventoryMenu) {
            Player owner = derk$resolveInventoryMenuOwner(inventoryMenu);
            if (owner != null) {
                worldPos = new NearbyInventoryScanner.WorldPos(owner.level(), owner.blockPosition());
            }
        }

        if (worldPos == null || worldPos.level().isClientSide()) {
            return;
        }

        List<Container> inventories = NearbyInventoryScanner.findNearbyInventories(
            worldPos.level(),
            worldPos.pos(),
            NearbyInventoryScanner.getConfiguredRadius()
        );

        for (Container inventory : inventories) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                contents.accountSimpleStack(stack);
            }
        }
    }

    private Player derk$resolveInventoryMenuOwner(InventoryMenu inventoryMenu) {
        try {
            Field ownerField = InventoryMenu.class.getDeclaredField("owner");
            ownerField.setAccessible(true);
            return (Player) ownerField.get(inventoryMenu);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
