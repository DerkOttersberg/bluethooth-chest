package com.derk.easyinventorycrafter.net;

import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record NearbyItemsPacket(List<NearbyItemEntry> entries, List<ItemStack> recipeFinderStacks) implements CustomPacketPayload {
    public static final Type<NearbyItemsPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("derk_easy_inventory_crafter", "nearby_items"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyItemsPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> packet.write(buf), NearbyItemsPacket::decode);

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

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (NearbyItemEntry entry : entries) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.stack());
            buf.writeVarInt(entry.count());
        }

        buf.writeVarInt(recipeFinderStacks.size());
        for (ItemStack stack : recipeFinderStacks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    @Override
    public Type<NearbyItemsPacket> type() {
        return TYPE;
    }
}
