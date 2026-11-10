package mekanism.fabric.shim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.TicketStorage;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Server side NeoForge hooks called from the mixins in {@code mekanism.fabric.mixin.hooks}.
 */
public final class ServerHooks {

    private ServerHooks() {
    }

    /**
     * Ticks the entity wrapped in NeoForge's entity tick events (cancelling the pre event skips the tick).
     */
    public static void tickWithEvents(Entity entity, Runnable tick) {
        if (!NeoForge.EVENT_BUS.post(new EntityTickEvent.Pre(entity)).isCanceled()) {
            tick.run();
            NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
        }
    }

    public static void fireChunkTicketLevelUpdated(ServerLevel level, long chunkPos, int oldTicketLevel, int newTicketLevel, @Nullable ChunkHolder chunkHolder) {
        if (oldTicketLevel != newTicketLevel) {
            NeoForge.EVENT_BUS.post(new ChunkTicketLevelUpdatedEvent(level, chunkPos, oldTicketLevel, newTicketLevel, chunkHolder));
        }
    }

    /**
     * Extends vanilla's ticket storage codec with the forced chunks of NeoForge's ticket controllers, like NeoForge's patch of {@link TicketStorage#CODEC}.
     */
    public static Codec<TicketStorage> extendTicketStorageCodec(Codec<TicketStorage> vanilla) {
        MapCodec<TicketStorage> vanillaMap = MapCodec.assumeMapUnsafe(vanilla);
        return RecordCodecBuilder.create(instance -> instance.group(
              vanillaMap.forGetter(Function.identity()),
              ForcedChunkManager.defineExtraStorageParams()
        ).apply(instance, (storage, ownedChunks) -> ForcedChunkManager.readStoredTickets(_ -> storage, List.of(), ownedChunks)));
    }

    /**
     * Reinstates the forced chunks of NeoForge's ticket controllers when a level is initially loaded.
     */
    public static void activateForcedChunks(ServerLevel level) {
        TicketStorage savedTickets = level.getDataStorage().get(TicketStorage.TYPE);
        if (savedTickets != null) {
            ForcedChunkManager.activateAllDeactivatedTickets(level, savedTickets);
        }
    }

    /**
     * Fires NeoForge's spawn placement registration event, starting from vanilla's spawn placements, and applies the result.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerSpawnPlacements() {
        Map<EntityType<?>, SpawnPlacements.Data> placements = SpawnPlacements.DATA_BY_TYPE;
        Map<EntityType<?>, RegisterSpawnPlacementsEvent.MergedSpawnPredicate<?>> merged = new HashMap<>();
        placements.forEach((type, data) -> merged.put(type, new RegisterSpawnPlacementsEvent.MergedSpawnPredicate(data.predicate(), data.placement(), data.heightMap())));
        ModLoader.postEvent(new RegisterSpawnPlacementsEvent(merged));
        merged.forEach((type, predicate) -> placements.put(type, new SpawnPlacements.Data(predicate.getHeightmapType(), predicate.getSpawnType(), predicate.build())));
    }
}
