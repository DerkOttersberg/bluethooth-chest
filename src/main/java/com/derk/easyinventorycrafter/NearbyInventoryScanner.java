package com.derk.easyinventorycrafter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public final class NearbyInventoryScanner {
    public static final int DEFAULT_RADIUS = 16;
    private static final int MAX_ENTRIES = 512;

    private NearbyInventoryScanner() {
    }

    public static int getConfiguredRadius() {
        return EasyInventoryCrafterConfig.getNearbyRadius();
    }

    public static List<Container> findNearbyInventories(Level level, BlockPos center, int radius) {
        Set<BlockEntity> seenEntities = new HashSet<>();
        List<Container> inventories = new ArrayList<>();

        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof Container inventory)) {
                continue;
            }
            if (inventory instanceof Inventory) {
                continue;
            }

            if (seenEntities.add(blockEntity)) {
                inventories.add(inventory);
            }
        }

        return inventories;
    }

    public static List<NearbyItemEntry> collectItemCounts(Level level, BlockPos center, int radius) {
        List<Container> inventories = findNearbyInventories(level, center, radius);
        Map<Item, Integer> totals = new HashMap<>();

        for (Container inventory : inventories) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    totals.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }
        }

        List<NearbyItemEntry> entries = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : totals.entrySet()) {
            entries.add(new NearbyItemEntry(new ItemStack(entry.getKey()), entry.getValue()));
            if (entries.size() >= MAX_ENTRIES) {
                break;
            }
        }

        return entries;
    }

    public static List<ItemStack> collectRecipeFinderStacks(Level level, BlockPos center, int radius) {
        List<Container> inventories = findNearbyInventories(level, center, radius);
        List<ItemStack> stacks = new ArrayList<>();

        for (Container inventory : inventories) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                stacks.add(stack.copy());
                if (stacks.size() >= MAX_ENTRIES) {
                    return stacks;
                }
            }
        }

        return stacks;
    }

    public static List<BlockPos> findInventoryPositionsWithItem(Level level, BlockPos center, int radius, Item item) {
        Set<BlockEntity> seenEntities = new HashSet<>();
        Set<BlockPos> positions = new LinkedHashSet<>();

        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isLoaded(pos)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof Container inventory)) {
                continue;
            }
            if (inventory instanceof Inventory || !seenEntities.add(blockEntity)) {
                continue;
            }

            if (inventoryHasItem(inventory, item)) {
                addHighlightPositions(level, pos, blockEntity, positions);
            }
        }

        return new ArrayList<>(positions);
    }

    private static void addHighlightPositions(Level level, BlockPos pos, BlockEntity blockEntity, Set<BlockPos> positions) {
        positions.add(pos.immutable());

        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return;
        }

        BlockPos connectedPos = derk$getConnectedChestPos(pos, state);
        if (!level.isLoaded(connectedPos)) {
            return;
        }

        BlockState connectedState = level.getBlockState(connectedPos);
        if (connectedState.getBlock() instanceof ChestBlock) {
            positions.add(connectedPos.immutable());
        }
    }

    private static BlockPos derk$getConnectedChestPos(BlockPos pos, BlockState state) {
        Direction connectedDirection = ChestBlock.getConnectedDirection(state);
        return pos.relative(connectedDirection);
    }

    @Nullable
    public static BlockPos findFirstInventoryPosWithItem(Level level, BlockPos center, int radius, Item item) {
        List<BlockPos> positions = findInventoryPositionsWithItem(level, center, radius, item);
        return positions.isEmpty() ? null : positions.getFirst();
    }

    private static boolean inventoryHasItem(Container inventory, Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    public record NearbyItemEntry(ItemStack stack, int count) {
    }

    public record WorldPos(Level level, BlockPos pos) {
    }
}
