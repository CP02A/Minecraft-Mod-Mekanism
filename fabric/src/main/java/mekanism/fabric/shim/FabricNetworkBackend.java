package mekanism.fabric.shim;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.shim.NetworkBackend;
import org.jspecify.annotations.Nullable;

/**
 * Implements NeoForge's payload registration and distribution with Fabric's networking API.
 */
public class FabricNetworkBackend implements NetworkBackend {

    /**
     * NeoForge splits large packets automatically, so allow payloads to be larger than vanilla's limit.
     */
    private static final int MAX_PAYLOAD_SIZE = 8 * 1024 * 1024;

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends CustomPacketPayload> void registerPlay(Direction direction, CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
          IPayloadHandler<T> handler, HandlerThread thread) {
        if (direction == Direction.TO_CLIENT) {
            PayloadTypeRegistry.clientboundPlay().registerLarge(type, (StreamCodec) codec, MAX_PAYLOAD_SIZE);
            if (FMLEnvironment.getDist() == Dist.CLIENT) {
                FabricClientNetworking.registerPlay(type, handler, thread);
            }
        } else {
            PayloadTypeRegistry.serverboundPlay().registerLarge(type, (StreamCodec) codec, MAX_PAYLOAD_SIZE);
            ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
                ServerPlayer player = context.player();
                handle(handler, payload, new Context(player, PacketFlow.SERVERBOUND, context.responseSender(), context.server(), thread));
            });
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends CustomPacketPayload> void registerConfiguration(Direction direction, CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec,
          IPayloadHandler<T> handler, HandlerThread thread) {
        if (direction == Direction.TO_CLIENT) {
            PayloadTypeRegistry.clientboundConfiguration().registerLarge(type, (StreamCodec) codec, MAX_PAYLOAD_SIZE);
            if (FMLEnvironment.getDist() == Dist.CLIENT) {
                FabricClientNetworking.registerConfiguration(type, handler, thread);
            }
        } else {
            PayloadTypeRegistry.serverboundConfiguration().registerLarge(type, (StreamCodec) codec, MAX_PAYLOAD_SIZE);
            ServerConfigurationNetworking.registerGlobalReceiver(type, (payload, context) ->
                  handle(handler, payload, new Context(null, PacketFlow.SERVERBOUND, context.responseSender(), context.server(), thread)));
        }
    }

    /**
     * Fires NeoForge's configuration task registration event whenever a player starts configuration, and adds the registered tasks.
     */
    public static void registerConfigurationTasks() {
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            RegisterConfigurationTasksEvent event = new RegisterConfigurationTasksEvent(handler);
            ModLoader.postEvent(event);
            for (ConfigurationTask task : event.getConfigurationTasks()) {
                handler.addTask(task);
            }
        });
    }

    static <T extends CustomPacketPayload> void handle(IPayloadHandler<T> handler, T payload, IPayloadContext context) {
        if (context instanceof Context ctx && ctx.thread == HandlerThread.MAIN && !ctx.executor.isSameThread()) {
            ctx.executor.execute(() -> handler.handle(payload, context));
        } else {
            handler.handle(payload, context);
        }
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToAllPlayers(CustomPacketPayload payload) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : PlayerLookup.all(server)) {
                sendToPlayer(player, payload);
            }
        }
    }

    @Override
    public void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload) {
        for (ServerPlayer player : PlayerLookup.level(level)) {
            sendToPlayer(player, payload);
        }
    }

    @Override
    public void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, CustomPacketPayload payload) {
        for (ServerPlayer player : PlayerLookup.tracking(level, chunkPos)) {
            sendToPlayer(player, payload);
        }
    }

    @Override
    public void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, boolean includeSelf) {
        for (ServerPlayer player : PlayerLookup.tracking(entity)) {
            sendToPlayer(player, payload);
        }
        if (includeSelf && entity instanceof ServerPlayer self) {
            sendToPlayer(self, payload);
        }
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        FabricClientNetworking.sendToServer(payload);
    }

    /**
     * NeoForge style payload context.
     */
    static final class Context implements IPayloadContext {

        @Nullable
        private final Player player;
        private final PacketFlow flow;
        private final PacketSender sender;
        final BlockableEventLoop<?> executor;
        final HandlerThread thread;

        Context(@Nullable Player player, PacketFlow flow, PacketSender sender, BlockableEventLoop<?> executor, HandlerThread thread) {
            this.player = player;
            this.flow = flow;
            this.sender = sender;
            this.executor = executor;
            this.thread = thread;
        }

        @Override
        public void reply(CustomPacketPayload payload) {
            sender.sendPacket(payload);
        }

        @Override
        public void disconnect(Component reason) {
            sender.disconnect(reason);
        }

        @Override
        public Player player() {
            if (player == null) {
                throw new UnsupportedOperationException("No player is available during the configuration phase");
            }
            return player;
        }

        @Override
        public PacketFlow flow() {
            return flow;
        }

        @Override
        public CompletableFuture<Void> enqueueWork(Runnable task) {
            if (executor.isSameThread()) {
                task.run();
                return CompletableFuture.completedFuture(null);
            }
            return executor.submit(task);
        }

        @Override
        public <T> CompletableFuture<T> enqueueWork(Supplier<T> task) {
            if (executor.isSameThread()) {
                return CompletableFuture.completedFuture(task.get());
            }
            return executor.submit(task);
        }
    }
}
