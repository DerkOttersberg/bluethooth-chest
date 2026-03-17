package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.PendingNearbyWithdrawal;
import com.derk.easyinventorycrafter.net.NearbyItemsSync;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin implements NearbyCraftingAccess {
    @Unique
    private final List<PendingNearbyWithdrawal> derk$pendingNearbyWithdrawals = new ArrayList<>();

    @Unique
    private final Map<Integer, Integer> derk$slotBaselineCounts = new HashMap<>();

    @Unique
    private boolean derk$reconcilingNearbyWithdrawals;

    @Unique
    private boolean derk$cancellingNearbyWithdrawals;

    @Unique
    private boolean derk$autofillingNearbyWithdrawals;

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Shadow
    @Final
    private Player player;

    @Shadow
    public abstract List<Slot> getInputGridSlots();

    @Shadow
    public abstract void slotsChanged(Container container);

    @Override
    public ContainerLevelAccess derk$getAccess() {
        return access;
    }

    @Override
    public Player derk$getPlayer() {
        return player;
    }

    @Override
    public void derk$recordNearbyWithdrawal(Container inventory, int sourceSlot, int craftingSlotIndex, ItemStack stack, int count, int baselineCount) {
        if (count <= 0 || craftingSlotIndex < 0) {
            return;
        }

        derk$slotBaselineCounts.putIfAbsent(craftingSlotIndex, baselineCount);
        derk$pendingNearbyWithdrawals.add(new PendingNearbyWithdrawal(
            inventory,
            sourceSlot,
            craftingSlotIndex,
            stack.copyWithCount(1),
            count
        ));
    }

    @Override
    public void derk$prepareNearbyWithdrawalsForAutofill() {
        derk$returnNearbyWithdrawals(false);
    }

    @Override
    public void derk$cancelNearbyWithdrawals() {
        derk$returnNearbyWithdrawals(true);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void derk$sendInitialNearbyItems(int syncId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            NearbyItemsSync.sendNearbyItems(serverPlayer);
        }
    }

    @Inject(method = "beginPlacingRecipe", at = @At("HEAD"))
    private void derk$markNearbyAutofillStart(CallbackInfo ci) {
        derk$autofillingNearbyWithdrawals = true;
    }

    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void derk$reconcileOnContentChanged(Container container, CallbackInfo ci) {
        if (!derk$cancellingNearbyWithdrawals && !derk$autofillingNearbyWithdrawals) {
            derk$reconcileNearbyWithdrawals();
        }
    }

    @Inject(method = "finishPlacingRecipe", at = @At("TAIL"))
    private void derk$refreshAfterFill(ServerLevel level, RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci) {
        derk$autofillingNearbyWithdrawals = false;
        derk$reconcileNearbyWithdrawals();
        if (player instanceof ServerPlayer serverPlayer) {
            NearbyItemsSync.sendNearbyItems(serverPlayer);
        }
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void derk$clearNearbyWithdrawals(Player player, CallbackInfo ci) {
        derk$pendingNearbyWithdrawals.clear();
        derk$slotBaselineCounts.clear();
    }

    @Unique
    private void derk$reconcileNearbyWithdrawals() {
        if (derk$reconcilingNearbyWithdrawals || derk$pendingNearbyWithdrawals.isEmpty()) {
            return;
        }

        derk$reconcilingNearbyWithdrawals = true;
        try {
            List<Slot> inputSlots = getInputGridSlots();
            Map<Integer, Integer> cancelableCounts = new HashMap<>();
            for (Integer slotIndex : derk$slotBaselineCounts.keySet()) {
                if (slotIndex < 0 || slotIndex >= inputSlots.size()) {
                    continue;
                }

                ItemStack slotStack = inputSlots.get(slotIndex).getItem();
                int baselineCount = derk$slotBaselineCounts.getOrDefault(slotIndex, 0);
                cancelableCounts.put(slotIndex, Math.max(0, slotStack.getCount() - baselineCount));
            }

            for (Iterator<PendingNearbyWithdrawal> iterator = derk$pendingNearbyWithdrawals.iterator(); iterator.hasNext();) {
                PendingNearbyWithdrawal withdrawal = iterator.next();
                if (withdrawal.craftingSlotIndex() < 0 || withdrawal.craftingSlotIndex() >= inputSlots.size()) {
                    iterator.remove();
                    continue;
                }

                ItemStack slotStack = inputSlots.get(withdrawal.craftingSlotIndex()).getItem();
                if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(slotStack, withdrawal.templateStack())) {
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

            derk$cleanupSlotBaselines();
        } finally {
            derk$reconcilingNearbyWithdrawals = false;
        }
    }

    @Unique
    private void derk$returnNearbyWithdrawals(boolean refreshAfter) {
        boolean changed = false;
        derk$cancellingNearbyWithdrawals = true;
        try {
            while (true) {
                derk$reconcileNearbyWithdrawals();
                if (derk$pendingNearbyWithdrawals.isEmpty()) {
                    break;
                }

                List<Slot> inputSlots = getInputGridSlots();
                boolean passChanged = false;
                for (Iterator<PendingNearbyWithdrawal> iterator = derk$pendingNearbyWithdrawals.iterator(); iterator.hasNext();) {
                    PendingNearbyWithdrawal withdrawal = iterator.next();
                    if (withdrawal.craftingSlotIndex() < 0 || withdrawal.craftingSlotIndex() >= inputSlots.size()) {
                        iterator.remove();
                        continue;
                    }

                    Slot slot = inputSlots.get(withdrawal.craftingSlotIndex());
                    ItemStack slotStack = slot.getItem();
                    if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(slotStack, withdrawal.templateStack())) {
                        iterator.remove();
                        continue;
                    }

                    int removableCount = Math.min(withdrawal.remainingCount(), slotStack.getCount());
                    if (removableCount <= 0) {
                        iterator.remove();
                        continue;
                    }

                    ItemStack toReturn = withdrawal.templateStack().copyWithCount(removableCount);
                    int insertedCount = derk$insertBackIntoInventory(withdrawal.sourceInventory(), withdrawal.sourceSlot(), toReturn);
                    if (insertedCount <= 0) {
                        continue;
                    }

                    if (insertedCount == slotStack.getCount()) {
                        slot.set(ItemStack.EMPTY);
                    } else {
                        slotStack.shrink(insertedCount);
                        slot.setChanged();
                    }

                    withdrawal.setRemainingCount(withdrawal.remainingCount() - insertedCount);
                    withdrawal.sourceInventory().setChanged();
                    passChanged = true;
                    changed = true;
                    if (withdrawal.remainingCount() <= 0) {
                        iterator.remove();
                    }
                }

                derk$cleanupSlotBaselines();
                if (!passChanged) {
                    break;
                }
            }
        } finally {
            derk$cancellingNearbyWithdrawals = false;
        }

        if (changed && refreshAfter) {
            derk$refreshAfterNearbyTransfer();
        }
    }

    @Unique
    private int derk$insertBackIntoInventory(Container inventory, int preferredSlot, ItemStack stack) {
        int inserted = 0;
        inserted += derk$tryInsertIntoSlot(inventory, preferredSlot, stack);
        for (int i = 0; i < inventory.getContainerSize() && !stack.isEmpty(); i++) {
            if (i == preferredSlot) {
                continue;
            }
            inserted += derk$tryInsertIntoSlot(inventory, i, stack);
        }
        return inserted;
    }

    @Unique
    private int derk$tryInsertIntoSlot(Container inventory, int slotIndex, ItemStack stack) {
        if (stack.isEmpty() || slotIndex < 0 || slotIndex >= inventory.getContainerSize() || !inventory.canPlaceItem(slotIndex, stack)) {
            return 0;
        }

        ItemStack targetStack = inventory.getItem(slotIndex);
        if (!targetStack.isEmpty() && !ItemStack.isSameItemSameComponents(targetStack, stack)) {
            return 0;
        }

        int maxCount = Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize(stack));
        if (maxCount <= 0) {
            return 0;
        }

        if (targetStack.isEmpty()) {
            int inserted = Math.min(stack.getCount(), maxCount);
            inventory.setItem(slotIndex, stack.copyWithCount(inserted));
            stack.shrink(inserted);
            return inserted;
        }

        int inserted = Math.min(stack.getCount(), maxCount - targetStack.getCount());
        if (inserted <= 0) {
            return 0;
        }

        targetStack.grow(inserted);
        stack.shrink(inserted);
        return inserted;
    }

    @Unique
    private void derk$cleanupSlotBaselines() {
        derk$slotBaselineCounts.entrySet().removeIf(entry -> derk$pendingNearbyWithdrawals.stream().noneMatch(withdrawal -> withdrawal.craftingSlotIndex() == entry.getKey()));
    }

    @Unique
    private void derk$refreshAfterNearbyTransfer() {
        List<Slot> inputSlots = getInputGridSlots();
        if (!inputSlots.isEmpty()) {
            slotsChanged(inputSlots.getFirst().container);
        }
        ((CraftingMenu) (Object) this).broadcastChanges();
        if (player instanceof ServerPlayer serverPlayer) {
            NearbyItemsSync.sendNearbyItems(serverPlayer);
        }
    }
}
