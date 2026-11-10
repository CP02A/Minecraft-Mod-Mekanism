package mekanism.fabric.shim;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;

/**
 * Client side of {@link FabricNetworkBackend}, only loaded on the client.
 */
final class FabricClientNetworking {

    private FabricClientNetworking() {
    }

    static <T extends CustomPacketPayload> void registerPlay(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler, HandlerThread thread) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> FabricNetworkBackend.handle(handler, payload,
              new FabricNetworkBackend.Context(context.player(), PacketFlow.CLIENTBOUND, context.responseSender(), context.client(), thread)));
    }

    static <T extends CustomPacketPayload> void registerConfiguration(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler, HandlerThread thread) {
        ClientConfigurationNetworking.registerGlobalReceiver(type, (payload, context) -> FabricNetworkBackend.handle(handler, payload,
              new FabricNetworkBackend.Context(null, PacketFlow.CLIENTBOUND, context.responseSender(), context.client(), thread)));
    }

    static void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
