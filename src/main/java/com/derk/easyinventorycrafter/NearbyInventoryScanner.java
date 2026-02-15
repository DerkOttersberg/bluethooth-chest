package com.derk.easyinventorycrafter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;

public final class NearbyInventoryScanner {
    public static final int DEFAULT_RADIUS = 16;
    private static final int MAX_ENTRIES = 512;

    private NearbyInventoryScanner() {}

    public static List<Storage<ItemVariant>> findNearbyStorages(
            World world, BlockPos center, int radius) {
        Set<Object> visitedIdentities =
                java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        List<Storage<ItemVariant>> storages = new ArrayList<>();
        BlockPos min = center.add(-radius, -radius, -radius);
        BlockPos max = center.add(radius, radius, radius);

        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (!world.isChunkLoaded(pos)) {
                continue;
            }

            Storage<ItemVariant> storage = getStorageAt(world, pos, visitedIdentities);
            if (storage != null) {
                storages.add(storage);
            }
        }

        return storages;
    }

    private static Storage<ItemVariant> getStorageAt(
            World world, BlockPos pos, Set<Object> visitedIdentities) {
        // 1. Try Fabric API Lookup (covers modded storage + vanilla blocks)
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, pos, null);
        if (storage != null) {
            // Deduplicate based on Storage identity
            if (visitedIdentities.add(storage)) {
                return storage;
            }
            return null;
        }

        // 2. Fallback to vanilla inventory check (covers entities like Chest Minecarts)
        Inventory inventory = HopperBlockEntity.getInventoryAt(world, pos);
        if (inventory != null && !(inventory instanceof PlayerInventory)) {
            // Deduplicate based on Inventory identity
            if (visitedIdentities.add(inventory)) {
                return InventoryStorage.of(inventory, null);
            }
        }

        return null;
    }

    public static List<NearbyItemEntry> collectItemCounts(
            World world, BlockPos center, int radius) {
        List<Storage<ItemVariant>> storages = findNearbyStorages(world, center, radius);
        Map<Item, Long> totals = new HashMap<>();

        for (Storage<ItemVariant> storage : storages) {
            for (StorageView<ItemVariant> view : storage) {
                if (!view.isResourceBlank()) {
                    totals.merge(view.getResource().getItem(), view.getAmount(), Long::sum);
                }
            }
        }

        List<NearbyItemEntry> entries = new ArrayList<>();
        for (Map.Entry<Item, Long> entry : totals.entrySet()) {
            Item item = entry.getKey();
            int count = (int) Math.min(entry.getValue(), Integer.MAX_VALUE);
            entries.add(new NearbyItemEntry(new ItemStack(item), count));
            if (entries.size() >= MAX_ENTRIES) {
                break;
            }
        }

        return entries;
    }

    public static List<BlockPos> findInventoryPositionsWithItem(
            World world, BlockPos center, int radius, Item item) {
        Set<Object> visitedCache = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        // Cache whether a specific storage/inventory contains the item to avoid re-scanning
        Map<Object, Boolean> hasItemCache = new IdentityHashMap<>();
        Set<BlockPos> positions = new LinkedHashSet<>();
        BlockPos min = center.add(-radius, -radius, -radius);
        BlockPos max = center.add(radius, radius, radius);

        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (!world.isChunkLoaded(pos)) {
                continue;
            }

            // We use a modified getStorageAt that doesn't consume the visit, but checks it
            Storage<ItemVariant> storage =
                    getStorageAndCacheConfig(world, pos, visitedCache, hasItemCache, item);

            if (storage != null) {
                // If we got a storage, it means it has the item (checked inside helper)
                positions.add(pos.toImmutable());
            }
        }

        return new ArrayList<>(positions);
    }

    private static Storage<ItemVariant> getStorageAndCacheConfig(
            World world,
            BlockPos pos,
            Set<Object> visitedIdentities,
            Map<Object, Boolean> hasItemCache,
            Item item) {
        // This is tricky: we want to map pos -> storage, then storage -> hasItem.
        // But we also want to return the storage ONLY if it has the item.
        // And we want to return it for ALL positions that map to this storage.

        // 1. Try SIDED
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(world, pos, null);
        Object key = storage;

        if (storage == null) {
            Inventory inventory = HopperBlockEntity.getInventoryAt(world, pos);
            if (inventory != null && !(inventory instanceof PlayerInventory)) {
                key = inventory;
                // We wrap it only if we need to check it
            } else {
                return null;
            }
        }

        // Check cache
        if (hasItemCache.containsKey(key)) {
            return hasItemCache.get(key)
                    ? (storage != null ? storage : InventoryStorage.of((Inventory) key, null))
                    : null;
        }

        // Not in cache, verify
        Storage<ItemVariant> toCheck = storage;
        if (toCheck == null) {
            toCheck = InventoryStorage.of((Inventory) key, null);
        }

        boolean has = false;
        for (StorageView<ItemVariant> view : toCheck) {
            if (view.getResource().getItem() == item && view.getAmount() > 0) {
                has = true;
                break;
            }
        }

        hasItemCache.put(key, has);
        return has ? toCheck : null;
    }

    public static WorldPos getWorldPos(ScreenHandlerContext context) {
        Optional<WorldPos> result = context.get((world, pos) -> new WorldPos(world, pos));
        return result.orElse(null);
    }

    public record NearbyItemEntry(ItemStack stack, int count) {}

    public record WorldPos(World world, BlockPos pos) {}
}
