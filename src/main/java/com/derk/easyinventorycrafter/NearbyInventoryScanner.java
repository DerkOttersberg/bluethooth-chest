package com.derk.easyinventorycrafter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

public final class NearbyInventoryScanner {
	public static final int DEFAULT_RADIUS = 16;
	private static final int MAX_ENTRIES = 512;

	private NearbyInventoryScanner() {
	}

	public static List<Inventory> findNearbyInventories(World world, BlockPos center, int radius) {
		Set<BlockEntity> seenEntities = new HashSet<>();
		List<Inventory> inventories = new ArrayList<>();

		BlockPos min = center.add(-radius, -radius, -radius);
		BlockPos max = center.add(radius, radius, radius);

		for (BlockPos pos : BlockPos.iterate(min, max)) {
			if (!world.isChunkLoaded(pos)) {
				continue;
			}

			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (!(blockEntity instanceof Inventory inventory)) {
				continue;
			}
			if (inventory instanceof PlayerInventory) {
				continue;
			}

			if (seenEntities.add(blockEntity)) {
				inventories.add(inventory);
			}
		}

		return inventories;
	}

	public static List<NearbyItemEntry> collectItemCounts(World world, BlockPos center, int radius) {
		List<Inventory> inventories = findNearbyInventories(world, center, radius);
		Map<Item, Integer> totals = new HashMap<>();

		for (Inventory inventory : inventories) {
			for (int i = 0; i < inventory.size(); i++) {
				ItemStack stack = inventory.getStack(i);
				if (!stack.isEmpty() && PlayerInventory.usableWhenFillingSlot(stack)) {
					totals.merge(stack.getItem(), stack.getCount(), Integer::sum);
				}
			}
		}

		List<NearbyItemEntry> entries = new ArrayList<>();
		for (Map.Entry<Item, Integer> entry : totals.entrySet()) {
			Item item = entry.getKey();
			int count = entry.getValue();
			entries.add(new NearbyItemEntry(new ItemStack(item), count));
			if (entries.size() >= MAX_ENTRIES) {
				break;
			}
		}

		return entries;
	}

	@Nullable
	public static BlockPos findFirstInventoryPosWithItem(World world, BlockPos center, int radius, Item item) {
		BlockPos min = center.add(-radius, -radius, -radius);
		BlockPos max = center.add(radius, radius, radius);

		Set<BlockEntity> seenEntities = new HashSet<>();

		for (BlockPos pos : BlockPos.iterate(min, max)) {
			if (!world.isChunkLoaded(pos)) {
				continue;
			}

			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (!(blockEntity instanceof Inventory inventory)) {
				continue;
			}
			if (inventory instanceof PlayerInventory) {
				continue;
			}
			if (!seenEntities.add(blockEntity)) {
				continue;
			}

			for (int i = 0; i < inventory.size(); i++) {
				ItemStack stack = inventory.getStack(i);
				if (!stack.isEmpty()
						&& stack.getItem() == item
						&& PlayerInventory.usableWhenFillingSlot(stack)) {
					return pos.toImmutable();
				}
			}
		}
		return null;
	}

	public static List<BlockPos> findInventoryPositionsWithItem(World world, BlockPos center, int radius, Item item) {
		Set<BlockEntity> seenEntities = new HashSet<>();
		Set<BlockPos> positions = new LinkedHashSet<>();

		BlockPos min = center.add(-radius, -radius, -radius);
		BlockPos max = center.add(radius, radius, radius);

		for (BlockPos pos : BlockPos.iterate(min, max)) {
			if (!world.isChunkLoaded(pos)) {
				continue;
			}

			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (!(blockEntity instanceof Inventory inventory)) {
				continue;
			}
			if (inventory instanceof PlayerInventory) {
				continue;
			}
			if (!seenEntities.add(blockEntity)) {
				continue;
			}

			if (inventoryHasItem(inventory, item)) {
				positions.add(pos.toImmutable());
			}
		}

		return new ArrayList<>(positions);
	}

	@Nullable
	public static WorldPos getWorldPos(ScreenHandlerContext context) {
		Optional<WorldPos> result = context.get((world, pos) -> new WorldPos(world, pos));
		return result.orElse(null);
	}

	public record NearbyItemEntry(ItemStack stack, int count) {
	}

	public record WorldPos(World world, BlockPos pos) {
	}

	private static boolean inventoryHasItem(Inventory inventory, Item item) {
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty()
					&& stack.getItem() == item
					&& PlayerInventory.usableWhenFillingSlot(stack)) {
				return true;
			}
		}
		return false;
	}
}
