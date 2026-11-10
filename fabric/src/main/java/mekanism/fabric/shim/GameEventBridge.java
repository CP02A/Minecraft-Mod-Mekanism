package mekanism.fabric.shim;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.capabilities.CapabilityHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.server.permission.PermissionAPI;

/**
 * Fires NeoForge's game events from the equivalent Fabric API events. Events without a Fabric equivalent are fired by mixins.
 */
public final class GameEventBridge {

    private GameEventBridge() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CapabilityHooks::invalidateCapsOnChunkLoad);
        NeoForge.EVENT_BUS.addListener(CapabilityHooks::invalidateCapsOnChunkUnload);
        NeoForge.EVENT_BUS.addListener(CapabilityHooks::cleanCapabilityListenerReferencesOnTick);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            ServerLifecycleHooks.setCurrentServer(server);
            PermissionAPI.initializePermissionAPI();
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> NeoForge.EVENT_BUS.post(new ServerStartedEvent(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            NeoForge.EVENT_BUS.post(new ServerStoppedEvent(server));
            ServerLifecycleHooks.setCurrentServer(null);
        });
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            MinecraftServer server = player.level().getServer();
            NeoForge.EVENT_BUS.post(new OnDatapackSyncEvent(server.getPlayerList(), player));
        });
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (client) {
                NeoForge.EVENT_BUS.post(new TagsUpdatedEvent.ClientPacketReceived(registries, ServerLifecycleHooks.getCurrentServer() != null));
            } else {
                //Note: The reloadable server resources are not exposed on Fabric
                NeoForge.EVENT_BUS.post(new TagsUpdatedEvent.ServerDataLoad(null, registries));
            }
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> NeoForge.EVENT_BUS.post(new ServerTickEvent.Pre(() -> true, server)));
        ServerTickEvents.END_SERVER_TICK.register(server -> NeoForge.EVENT_BUS.post(new ServerTickEvent.Post(() -> true, server)));
        ServerTickEvents.START_LEVEL_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Pre(() -> true, level)));
        ServerTickEvents.END_LEVEL_TICK.register(level -> NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(() -> true, level)));

        ServerLevelEvents.LOAD.register((server, level) -> NeoForge.EVENT_BUS.post(new LevelEvent.Load(level)));
        ServerLevelEvents.UNLOAD.register((server, level) -> NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level)));
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newChunk) -> NeoForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, newChunk)));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> NeoForge.EVENT_BUS.post(new ChunkEvent.Unload(chunk)));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> NeoForge.EVENT_BUS.post(new EntityJoinLevelEvent(entity, level)));

        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> NeoForge.EVENT_BUS.post(new RegisterCommandsEvent(dispatcher, selection, context)));

        ServerPlayerEvents.JOIN.register(player -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(handler.player)));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            FabricAttachmentBackend.copyOnRespawn(oldPlayer, newPlayer, alive);
            NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(newPlayer, oldPlayer, !alive));
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(newPlayer, alive)));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
              NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, origin.dimension(), destination.dimension())));
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> NeoForge.EVENT_BUS.post(new PlayerEvent.StartTracking(player, entity)));
        EntityTrackingEvents.STOP_TRACKING.register((entity, player) -> NeoForge.EVENT_BUS.post(new PlayerEvent.StopTracking(player, entity)));

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !NeoForge.EVENT_BUS.post(new LivingDeathEvent(entity, source)).isCanceled());
        UseBlockCallback.EVENT.register(GameEventBridge::onUseBlock);
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
              !NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos, state, player)).isCanceled());
    }

    /**
     * Fires NeoForge's {@link PlayerInteractEvent.RightClickBlock}. As Fabric's callback can only replace the whole interaction, forcing the block to be
     * used (for example while sneaking) is done by performing the block interaction here.
     */
    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        PlayerInteractEvent.RightClickBlock event = NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickBlock(player, hand, hit.getBlockPos(), hit));
        if (event.isCanceled()) {
            return event.getCancellationResult();
        }
        if (event.getUseBlock() == TriState.TRUE) {
            BlockState state = level.getBlockState(hit.getBlockPos());
            ItemStack stack = player.getItemInHand(hand);
            InteractionResult result = state.useItemOn(stack, level, player, hand, hit);
            if (result.consumesAction()) {
                return result;
            }
            if (result instanceof InteractionResult.TryEmptyHandInteraction && hand == InteractionHand.MAIN_HAND) {
                result = state.useWithoutItem(level, player, hit);
                if (result.consumesAction()) {
                    return result;
                }
            }
        }
        //Note: Preventing only the item use (TriState.FALSE) isn't supported, as the block has been used at this point already
        return InteractionResult.PASS;
    }
}
