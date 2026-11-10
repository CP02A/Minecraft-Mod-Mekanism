package mekanism.fabric.gametest;

import java.util.HashSet;
import java.util.Set;
import mekanism.common.tile.component.TileComponentChunkLoader;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Tests the server side NeoForge hooks: forced chunks of ticket controllers (and their persistence), ticket level events, spawn placements and entity
 * tick events.
 */
public class ServerHookTests {

    private static final Set<Long> LOADED_CHUNKS = new HashSet<>();
    private static final Set<Integer> TICKED_ENTITIES = new HashSet<>();
    private static boolean listenersRegistered;

    private static synchronized void registerListeners() {
        if (!listenersRegistered) {
            listenersRegistered = true;
            NeoForge.EVENT_BUS.addListener(ChunkTicketLevelUpdatedEvent.class, event -> {
                if (ChunkLevel.isLoaded(event.getNewTicketLevel())) {
                    LOADED_CHUNKS.add(event.getChunkPos());
                }
            });
            NeoForge.EVENT_BUS.addListener(EntityTickEvent.Post.class, event -> {
                if (!event.getEntity().level().isClientSide()) {
                    TICKED_ENTITIES.add(event.getEntity().getId());
                }
            });
        }
    }

    @GameTest(maxTicks = 200)
    public void forcedChunksArePersisted(GameTestHelper helper) {
        registerListeners();
        ServerLevel level = helper.getLevel();
        BlockPos owner = helper.absolutePos(new BlockPos(1, 1, 1));
        ChunkPos chunk = ChunkPos.containing(owner);
        int chunkX = chunk.x() + 20, chunkZ = chunk.z() + 20;
        long packed = ChunkPos.pack(chunkX, chunkZ);
        helper.assertTrue(TileComponentChunkLoader.TICKET_CONTROLLER.forceChunk(level, owner, chunkX, chunkZ, true, false),
              Component.literal("Expected Mekanism's ticket controller to force the chunk"));
        helper.succeedWhen(() -> {
            helper.assertTrue(LOADED_CHUNKS.contains(packed), Component.literal("Expected a ticket level update event for the forced chunk"));
            TicketStorage storage = level.getDataStorage().get(TicketStorage.TYPE);
            helper.assertTrue(storage != null && ForcedChunkManager.hasForcedChunks(level), Component.literal("Expected the level to have forced chunks"));
            //Save and load the ticket storage
            var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag saved = TicketStorage.CODEC.encodeStart(ops, storage).getOrThrow();
            TicketStorage loaded = TicketStorage.CODEC.parse(ops, saved).getOrThrow();
            helper.assertFalse(loaded.getBlockForcedChunks().hasNoDeactivatedTickets(), Component.literal("Expected the forced chunk to be loaded (deactivated)"));
            //There is no chunk loader at the owner's position, so Mekanism's validation callback should release the ticket when it gets reinstated
            ForcedChunkManager.activateAllDeactivatedTickets(level, loaded);
            helper.assertTrue(loaded.getBlockForcedChunks().hasNoDeactivatedTickets() && loaded.getBlockForcedChunks().isEmpty(),
                  Component.literal("Expected Mekanism's validation callback to release the ticket of the missing chunk loader"));
            TileComponentChunkLoader.TICKET_CONTROLLER.forceChunk(level, owner, chunkX, chunkZ, false, false);
        });
    }

    @GameTest
    public void vanillaSpawnPlacementsAreKept(GameTestHelper helper) {
        helper.assertTrue(SpawnPlacements.getPlacementType(EntityTypes.COD) == SpawnPlacementTypes.IN_WATER,
              Component.literal("Vanilla's spawn placements should be kept after firing RegisterSpawnPlacementsEvent"));
        helper.succeed();
    }

    @GameTest
    public void passengersFireEntityTickEvents(GameTestHelper helper) {
        registerListeners();
        Minecart minecart = helper.spawn(EntityTypes.MINECART, new BlockPos(1, 2, 1));
        Pig pig = helper.spawn(EntityTypes.PIG, new BlockPos(1, 2, 1));
        helper.assertTrue(pig.startRiding(minecart, true, false), Component.literal("The pig should ride the minecart"));
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.isPassenger(), Component.literal("The pig should still be riding"));
            helper.assertTrue(TICKED_ENTITIES.contains(pig.getId()), Component.literal("Expected an entity tick event for the riding pig"));
            helper.assertTrue(TICKED_ENTITIES.contains(minecart.getId()), Component.literal("Expected an entity tick event for the minecart"));
        });
    }
}
