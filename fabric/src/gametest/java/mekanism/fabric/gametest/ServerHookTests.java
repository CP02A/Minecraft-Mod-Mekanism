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
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Tests the server side NeoForge hooks: forced chunks of ticket controllers (and their persistence), ticket level events, spawn placements and entity
 * tick events, FinalizeSpawnEvent spawn cancellation, spawn placement checks and adding to existing creative tabs.
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
            //Husks and strays are only used by these tests
            NeoForge.EVENT_BUS.addListener(FinalizeSpawnEvent.class, event -> {
                if (event.getEntity().getType() == EntityTypes.HUSK) {
                    event.setSpawnCancelled(true);
                }
            });
            NeoForge.EVENT_BUS.addListener(MobSpawnEvent.SpawnPlacementCheck.class, event -> {
                if (event.getEntityType() == EntityTypes.STRAY) {
                    event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
                } else if (event.getEntityType() == EntityTypes.HUSK) {
                    event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
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

    @GameTest
    public void finalizeSpawnCancellation(GameTestHelper helper) {
        registerListeners();
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.assertTrue(EntityTypes.HUSK.spawn(helper.getLevel(), helper.absolutePos(pos), EntitySpawnReason.SPAWN_ITEM_USE) == null,
              Component.literal("Spawning a husk should be cancelled by the FinalizeSpawnEvent listener"));
        helper.assertEntityNotPresent(EntityTypes.HUSK);
        //Not cancelled
        helper.assertTrue(EntityTypes.ZOMBIE.spawn(helper.getLevel(), helper.absolutePos(pos), EntitySpawnReason.SPAWN_ITEM_USE) != null,
              Component.literal("Spawning a zombie shouldn't be cancelled"));
        helper.assertEntityPresent(EntityTypes.ZOMBIE);
        helper.succeed();
    }

    @GameTest
    public void spawnPlacementCheck(GameTestHelper helper) {
        registerListeners();
        ServerLevel level = helper.getLevel();
        //Air isn't a valid spawn position for monsters
        BlockPos air = helper.absolutePos(new BlockPos(1, 3, 1));
        helper.assertFalse(SpawnPlacements.checkSpawnRules(EntityTypes.ZOMBIE, level, EntitySpawnReason.NATURAL, air, level.getRandom()),
              Component.literal("Zombies shouldn't be able to spawn in mid air"));
        helper.assertTrue(SpawnPlacements.checkSpawnRules(EntityTypes.HUSK, level, EntitySpawnReason.NATURAL, air, level.getRandom()),
              Component.literal("The SpawnPlacementCheck listener should allow husks to spawn anywhere"));
        helper.assertFalse(SpawnPlacements.checkSpawnRules(EntityTypes.STRAY, level, EntitySpawnReason.SPAWNER, air, level.getRandom()),
              Component.literal("The SpawnPlacementCheck listener should prevent strays from spawning"));
        helper.succeed();
    }

    @GameTest
    public void addsToExistingCreativeTabs(GameTestHelper helper) {
        //Mekanism adds its ores to the vanilla natural blocks tab in BuildCreativeModeTabContentsEvent
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.NATURAL_BLOCKS);
        ServerLevel level = helper.getLevel();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(), true, level.registryAccess()));
        helper.assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> stack.is(MekanismBlocks.SALT_BLOCK.asItem())),
              Component.literal("The salt block should be in the natural blocks tab"));
        //Note: Mekanism adds to existing tabs with PARENT_TAB_ONLY (it is in the search tab through its own tab), so check vanilla's entries are kept
        helper.assertTrue(tab.getSearchTabDisplayItems().stream().anyMatch(stack -> stack.is(net.minecraft.world.item.Items.STONE)),
              Component.literal("Vanilla's search entries of the natural blocks tab should be kept"));
        helper.succeed();
    }
}
