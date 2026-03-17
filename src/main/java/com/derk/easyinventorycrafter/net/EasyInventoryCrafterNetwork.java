package com.derk.easyinventorycrafter.net;

import com.derk.easyinventorycrafter.EasyInventoryCrafterMod;
import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class EasyInventoryCrafterNetwork {
    private static final SimpleChannel CHANNEL = ChannelBuilder
        .named(EasyInventoryCrafterMod.MOD_ID + ":main")
        .networkProtocolVersion(1)
        .simpleChannel();

    private EasyInventoryCrafterNetwork() {
    }

    public static void init() {
        CHANNEL.messageBuilder(RequestNearbyItemsPacket.class, 0)
            .encoder(RequestNearbyItemsPacket::encode)
            .decoder(RequestNearbyItemsPacket::decode)
            .consumerMainThread(EasyInventoryCrafterNetwork::handleRequestNearbyItems)
            .add();

        CHANNEL.messageBuilder(NearbyItemsPacket.class, 1)
            .encoder((packet, buf) -> NearbyItemsPacket.encode(packet, (RegistryFriendlyByteBuf) buf))
            .decoder(buf -> NearbyItemsPacket.decode((RegistryFriendlyByteBuf) buf))
            .consumerMainThread(EasyInventoryCrafterNetwork::handleNearbyItems)
            .add();

        CHANNEL.messageBuilder(NearbyHighlightRequestPacket.class, 2)
            .encoder((packet, buf) -> NearbyHighlightRequestPacket.encode(packet, (RegistryFriendlyByteBuf) buf))
            .decoder(buf -> NearbyHighlightRequestPacket.decode((RegistryFriendlyByteBuf) buf))
            .consumerMainThread(EasyInventoryCrafterNetwork::handleHighlightRequest)
            .add();

        CHANNEL.messageBuilder(NearbyHighlightResponsePacket.class, 3)
            .encoder(NearbyHighlightResponsePacket::encode)
            .decoder(NearbyHighlightResponsePacket::decode)
            .consumerMainThread(EasyInventoryCrafterNetwork::handleHighlightResponse)
            .add();

        CHANNEL.messageBuilder(ReturnNearbyItemsPacket.class, 4)
            .encoder(ReturnNearbyItemsPacket::encode)
            .decoder(ReturnNearbyItemsPacket::decode)
            .consumerMainThread(EasyInventoryCrafterNetwork::handleReturnNearbyItems)
            .add();

        CHANNEL.build();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToServer(Object packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }

    private static void handleRequestNearbyItems(RequestNearbyItemsPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        if (player.containerMenu instanceof CraftingMenu || player.containerMenu instanceof InventoryMenu) {
            NearbyItemsSync.sendNearbyItems(player);
        }
    }

    private static void handleHighlightRequest(NearbyHighlightRequestPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        if (!(player.containerMenu instanceof CraftingMenu) && !(player.containerMenu instanceof InventoryMenu)) {
            return;
        }

        List<net.minecraft.core.BlockPos> positions = NearbyItemsSync.findHighlightPositions(player, packet.stack());
        if (positions != null && !positions.isEmpty()) {
            sendToPlayer(player, new NearbyHighlightResponsePacket(positions));
        }
    }

    private static void handleReturnNearbyItems(ReturnNearbyItemsPacket packet, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        if (player.containerMenu instanceof NearbyCraftingAccess access) {
            access.derk$cancelNearbyWithdrawals();
        }
    }

    private static void handleNearbyItems(NearbyItemsPacket packet, CustomPayloadEvent.Context context) {
        NearbyItemsClientState.applyPayload(packet);
    }

    private static void handleHighlightResponse(NearbyHighlightResponsePacket packet, CustomPayloadEvent.Context context) {
        NearbyItemsClientState.setHighlight(packet.positions(), com.derk.easyinventorycrafter.EasyInventoryCrafterConfig.getHighlightDurationTicks());
    }
}
