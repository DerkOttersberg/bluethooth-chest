package com.derk.easyinventorycrafter.net;

import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record NearbyItemsPacket(List<NearbyItemEntry> entries, List<ItemStack> recipeFinderStacks) {
    public static NearbyItemsPacket decode(RegistryFriendlyByteBuf buf) {
        int entryCount = buf.readVarInt();
        List<NearbyItemEntry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            int count = buf.readVarInt();
            entries.add(new NearbyItemEntry(stack, count));
        }

        int stackCount = buf.readVarInt();
        List<ItemStack> recipeFinderStacks = new ArrayList<>(stackCount);
        for (int i = 0; i < stackCount; i++) {
            recipeFinderStacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }

        return new NearbyItemsPacket(entries, recipeFinderStacks);
    }

    public static void encode(NearbyItemsPacket packet, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(packet.entries().size());
        for (NearbyItemEntry entry : packet.entries()) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.stack());
            buf.writeVarInt(entry.count());
        }

        buf.writeVarInt(packet.recipeFinderStacks().size());
        for (ItemStack stack : packet.recipeFinderStacks()) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }
}
