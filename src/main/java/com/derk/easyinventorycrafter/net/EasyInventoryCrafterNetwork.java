package com.derk.easyinventorycrafter.net;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class EasyInventoryCrafterNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private EasyInventoryCrafterNetwork() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(EasyInventoryCrafterNetwork::registerPayloadHandlers);
    }

    public static void initClient(IEventBus modBus) {
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(RequestNearbyItemsPacket.TYPE, RequestNearbyItemsPacket.STREAM_CODEC, EasyInventoryCrafterNetwork::handleRequestNearbyItems);
        registrar.playToClient(NearbyItemsPacket.TYPE, NearbyItemsPacket.STREAM_CODEC, EasyInventoryCrafterNetwork::handleNearbyItems);
        registrar.playToServer(NearbyHighlightRequestPacket.TYPE, NearbyHighlightRequestPacket.STREAM_CODEC, EasyInventoryCrafterNetwork::handleHighlightRequest);
        registrar.playToClient(NearbyHighlightResponsePacket.TYPE, NearbyHighlightResponsePacket.STREAM_CODEC, EasyInventoryCrafterNetwork::handleHighlightResponse);
        registrar.playToServer(ReturnNearbyItemsPacket.TYPE, ReturnNearbyItemsPacket.STREAM_CODEC, EasyInventoryCrafterNetwork::handleReturnNearbyItems);
    }

    public static void sendToPlayer(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    private static void handleRequestNearbyItems(RequestNearbyItemsPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (player.containerMenu instanceof CraftingMenu || player.containerMenu instanceof InventoryMenu) {
            NearbyItemsSync.sendNearbyItems(player);
        }
    }

    private static void handleHighlightRequest(NearbyHighlightRequestPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
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

    private static void handleReturnNearbyItems(ReturnNearbyItemsPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        if (player.containerMenu instanceof NearbyCraftingAccess access) {
            access.derk$cancelNearbyWithdrawals();
        }
    }

    private static void handleNearbyItems(NearbyItemsPacket packet, IPayloadContext context) {
        NearbyItemsClientState.applyPayload(packet);
    }

    private static void handleHighlightResponse(NearbyHighlightResponsePacket packet, IPayloadContext context) {
        NearbyItemsClientState.setHighlight(packet.positions(), com.derk.easyinventorycrafter.EasyInventoryCrafterConfig.getHighlightDurationTicks());
    }
}
