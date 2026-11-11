package mekanism.fabric.gametest;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import mekanism.api.AutomationType;
import mekanism.api.MekanismRegistries;
import mekanism.api.chemical.ChemicalIds;
import mekanism.api.chemical.ChemicalResource;
import mekanism.api.security.SecurityMode;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.command.builders.Builders;
import mekanism.common.command.builders.StructureBuilder;
import mekanism.common.config.IMekanismConfig;
import mekanism.common.config.MekanismConfig;
import mekanism.common.config.MekanismConfigType;
import mekanism.common.content.boiler.BoilerMultiblockData;
import mekanism.common.content.matrix.MatrixMultiblockData;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.content.tank.TankMultiblockData;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.item.ItemConfigurator.ConfiguratorMode;
import mekanism.common.lib.frequency.Frequency.FrequencyIdentity;
import mekanism.common.lib.frequency.FrequencyTypes;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.lib.transmitter.ConnectionType;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.registries.MekanismItems;
import mekanism.common.tile.TileEntityChemicalTank;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.TileEntityTeleporter;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import mekanism.common.tile.multiblock.TileEntityInductionPort;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.common.tile.qio.TileEntityQIODriveArray;
import mekanism.common.tile.qio.TileEntityQIOImporter;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Gameplay coverage of Mekanism's main module on Fabric: multiblocks, chemical transfer, the digital miner, QIO, teleporters, item interactions,
 * persistence and configs.
 */
public class GameplayTests {

    /**
     * Places a block like a player would (applying the item's default components, such as the default side configuration).
     */
    private static <BE extends BlockEntity> BE place(GameTestHelper helper, BlockPos pos, BlockRegistryObject<?, ?> block, Direction facing, Class<BE> beClass) {
        BlockState state = Attribute.setFacing(block.defaultState(), facing);
        helper.setBlock(pos, state == null ? block.defaultState() : state);
        BE blockEntity = helper.getBlockEntity(pos, beClass);
        blockEntity.applyComponentsFromItemStack(new ItemStack(block));
        return blockEntity;
    }

    private static ChemicalResource hydrogen(GameTestHelper helper) {
        return ChemicalResource.of(helper.getLevel().registryAccess().lookupOrThrow(MekanismRegistries.Keys.CHEMICAL).getOrThrow(ChemicalIds.HYDROGEN));
    }

    private static MultiblockData multiblock(GameTestHelper helper, BlockPos pos) {
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(pos)) instanceof TileEntityMultiblock<?> tile)) {
            throw helper.assertionException(pos, "Expected a multiblock tile");
        }
        return tile.getMultiblock();
    }

    private static void assertFormed(GameTestHelper helper, BlockPos pos, String name) {
        helper.assertTrue(multiblock(helper, pos).isFormed(), Component.literal("The " + name + " should form"));
    }

    private static ServerPlayer player(GameTestHelper helper, ItemStack held) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return player;
    }

    /**
     * Right-clicks a block like a player (through {@code ServerPlayerGameMode#useItemOn}, which is what the use item on packet calls).
     */
    private static InteractionResult useOn(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        player.setPos(Vec3.atCenterOf(absolute.relative(face, 2)));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
        return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
    }

    /**
     * A cuboid multiblock of the given size (the build command's builders are 18x18x18), with extra blocks placed by a callback.
     */
    private static class CuboidBuilder extends StructureBuilder {

        private final BlockState casing;
        private final BlockState wall;
        private final boolean openTop;
        private final BiConsumer<Level, BlockPos> extras;

        private CuboidBuilder(int size, int height, BlockRegistryObject<?, ?> casing, BlockRegistryObject<?, ?> wall, boolean openTop, BiConsumer<Level, BlockPos> extras) {
            super(size, height, size);
            this.casing = casing.defaultState();
            this.wall = wall.defaultState();
            this.openTop = openTop;
            this.extras = extras;
        }

        @Override
        protected void build(Level world, BlockPos start, boolean empty) {
            buildFrame(world, start);
            buildWalls(world, start);
            buildInteriorLayers(world, start, 1, openTop ? sizeY - 1 : sizeY - 2, Blocks.AIR.defaultBlockState());
            extras.accept(world, start);
        }

        @Override
        protected BlockState getWallBlock(BlockPos pos) {
            return wall;
        }

        @Override
        protected BlockState getCasing() {
            return casing;
        }

        private void build(GameTestHelper helper, BlockPos start) {
            build(helper.getLevel(), helper.absolutePos(start), false);
        }
    }

    @GameTest(maxTicks = 200)
    public void dynamicTankFormsAndStoresFluids(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 1, 1);
        BlockPos valve = start.offset(1, 1, 0);
        new CuboidBuilder(3, 3, MekanismBlocks.DYNAMIC_TANK, MekanismBlocks.STRUCTURAL_GLASS, false,
              (level, s) -> level.setBlockAndUpdate(s.offset(1, 1, 0), MekanismBlocks.DYNAMIC_VALVE.defaultState())).build(helper, start);
        helper.startSequence()
              .thenWaitUntil(() -> assertFormed(helper, valve, "dynamic tank"))
              .thenExecute(() -> {
                  ResourceHandler<FluidResource> fluids = Capabilities.Fluid.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(valve), null, null, Direction.NORTH);
                  helper.assertTrue(fluids != null, Component.literal("Expected the dynamic valve to expose a fluid handler"));
                  try (Transaction transaction = Transaction.openRoot()) {
                      helper.assertValueEqual(fluids.insert(FluidResource.of(Fluids.WATER), 5_000, transaction), 5_000, "inserted water");
                      transaction.commit();
                  }
                  TankMultiblockData tank = (TankMultiblockData) multiblock(helper, valve);
                  helper.assertValueEqual(tank.getFluidTank().amountAsLong(), 5_000L, "water stored in the dynamic tank");
                  try (Transaction transaction = Transaction.openRoot()) {
                      helper.assertValueEqual(fluids.extract(FluidResource.of(Fluids.WATER), 1_000, transaction), 1_000, "extracted water");
                      transaction.commit();
                  }
              })
              .thenSucceed();
    }

    @GameTest(maxTicks = 300)
    public void inductionMatrixChargesAndDischarges(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 1, 1);
        BlockPos port = start.offset(1, 1, 0);
        BlockPos cube = port.north();
        new CuboidBuilder(4, 4, MekanismBlocks.INDUCTION_CASING, MekanismBlocks.STRUCTURAL_GLASS, false, (level, s) -> {
            for (int x = 1; x <= 2; x++) {
                for (int z = 1; z <= 2; z++) {
                    level.setBlockAndUpdate(s.offset(x, 1, z), MekanismBlocks.BASIC_INDUCTION_CELL.defaultState());
                    level.setBlockAndUpdate(s.offset(x, 2, z), MekanismBlocks.BASIC_INDUCTION_PROVIDER.defaultState());
                }
            }
            level.setBlockAndUpdate(s.offset(1, 1, 0), MekanismBlocks.INDUCTION_PORT.defaultState());
        }).build(helper, start);
        helper.startSequence()
              .thenWaitUntil(() -> assertFormed(helper, port, "induction matrix"))
              .thenExecute(() -> {
                  EnergyHandler energy = Capabilities.Energy.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(port), null, null, Direction.NORTH);
                  helper.assertTrue(energy != null, Component.literal("Expected the induction port to expose an energy handler"));
                  try (Transaction transaction = Transaction.openRoot()) {
                      helper.assertTrue(energy.insert(100_000, transaction) > 0, Component.literal("The induction port should accept energy in input mode"));
                      transaction.commit();
                  }
              })
              .thenWaitUntil(() -> helper.assertTrue(((MatrixMultiblockData) multiblock(helper, port)).getEnergy() > 0,
                    Component.literal("The induction matrix should store the inserted energy")))
              .thenExecute(() -> {
                  //Switch the port to output mode and place an energy cube in front of it (it accepts energy from its back)
                  helper.getBlockEntity(port, TileEntityInductionPort.class).setActive(true);
                  place(helper, cube, MekanismBlocks.BASIC_ENERGY_CUBE, Direction.NORTH, TileEntityEnergyCube.class);
              })
              .thenWaitUntil(() -> helper.assertTrue(helper.getBlockEntity(cube, TileEntityEnergyCube.class).energyContainer().getAmountAsLong() > 0,
                    Component.literal("The induction port should output energy into the energy cube")))
              .thenSucceed();
    }

    @GameTest(maxTicks = 200)
    public void thermalEvaporationPlantFormsAndAcceptsWater(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 1, 1);
        BlockPos controller = start.offset(1, 1, 0);
        BlockPos valve = start.offset(2, 1, 0);
        new CuboidBuilder(4, 3, MekanismBlocks.THERMAL_EVAPORATION_BLOCK, MekanismBlocks.THERMAL_EVAPORATION_BLOCK, true, (level, s) -> {
            level.setBlockAndUpdate(s.offset(1, 1, 0), MekanismBlocks.THERMAL_EVAPORATION_CONTROLLER.defaultState());
            level.setBlockAndUpdate(s.offset(2, 1, 0), MekanismBlocks.THERMAL_EVAPORATION_VALVE.defaultState());
        }).build(helper, start);
        helper.startSequence()
              .thenWaitUntil(() -> assertFormed(helper, controller, "thermal evaporation plant"))
              .thenExecute(() -> {
                  ResourceHandler<FluidResource> fluids = Capabilities.Fluid.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(valve), null, null, Direction.NORTH);
                  helper.assertTrue(fluids != null, Component.literal("Expected the evaporation valve to expose a fluid handler"));
                  try (Transaction transaction = Transaction.openRoot()) {
                      helper.assertValueEqual(fluids.insert(FluidResource.of(Fluids.WATER), 1_000, transaction), 1_000, "inserted water");
                      transaction.commit();
                  }
              })
              .thenSucceed();
    }

    @GameTest(maxTicks = 200)
    public void boilerFormsAndAcceptsWater(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 1, 1);
        BlockPos valve = start.offset(1, 2, 0);
        new CuboidBuilder(3, 6, MekanismBlocks.BOILER_CASING, MekanismBlocks.STRUCTURAL_GLASS, false, (level, s) -> {
            level.setBlockAndUpdate(s.offset(1, 1, 1), MekanismBlocks.SUPERHEATING_ELEMENT.defaultState());
            level.setBlockAndUpdate(s.offset(1, 3, 1), MekanismBlocks.PRESSURE_DISPERSER.defaultState());
            level.setBlockAndUpdate(s.offset(1, 2, 0), MekanismBlocks.BOILER_VALVE.defaultState());
        }).build(helper, start);
        helper.startSequence()
              .thenWaitUntil(() -> assertFormed(helper, valve, "boiler"))
              .thenExecute(() -> {
                  ResourceHandler<FluidResource> fluids = Capabilities.Fluid.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(valve), null, null, Direction.NORTH);
                  helper.assertTrue(fluids != null, Component.literal("Expected the boiler valve to expose a fluid handler"));
                  try (Transaction transaction = Transaction.openRoot()) {
                      helper.assertTrue(fluids.insert(FluidResource.of(Fluids.WATER), 1_000, transaction) > 0, Component.literal("The boiler should accept water"));
                      transaction.commit();
                  }
                  helper.assertTrue(((BoilerMultiblockData) multiblock(helper, valve)).waterTank.amountAsLong() > 0, Component.literal("The boiler should store water"));
              })
              .thenSucceed();
    }

    @GameTest(maxTicks = 200)
    public void spsForms(GameTestHelper helper) {
        BlockPos start = new BlockPos(0, 1, 0);
        new Builders.SPSBuilder() {{
            build(helper.getLevel(), helper.absolutePos(start), false);
        }};
        //The SPS builder doesn't place the corners, so check a casing in the middle of an edge
        helper.succeedWhen(() -> assertFormed(helper, start.offset(3, 0, 0), "supercritical phase shifter"));
    }

    @GameTest(maxTicks = 200)
    public void chemicalsFlowThroughPressurizedTubes(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 1);
        BlockPos tube = source.south();
        BlockPos target = tube.south();
        ChemicalResource hydrogen = hydrogen(helper);
        TileEntityChemicalTank sourceTank = place(helper, source, MekanismBlocks.BASIC_CHEMICAL_TANK, Direction.SOUTH, TileEntityChemicalTank.class);
        place(helper, target, MekanismBlocks.BASIC_CHEMICAL_TANK, Direction.SOUTH, TileEntityChemicalTank.class);
        helper.setBlock(tube, MekanismBlocks.BASIC_PRESSURIZED_TUBE.defaultState());
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(sourceTank.getChemicalTank().insert(hydrogen, 1_000, transaction, AutomationType.INTERNAL), 1_000, "inserted hydrogen");
            transaction.commit();
        }
        helper.startSequence()
              .thenIdle(2)
              .thenExecute(() -> {
                  Transmitter<?, ?, ?> transmitter = helper.getBlockEntity(tube, TileEntityTransmitter.class).getTransmitter();
                  //Pull from the source tank's output side (its front)
                  transmitter.setConnectionTypeRaw(Direction.NORTH, ConnectionType.PULL);
              })
              .thenWaitUntil(() -> helper.assertTrue(helper.getBlockEntity(target, TileEntityChemicalTank.class).getChemicalTank().amountAsLong() > 0,
                    Component.literal("Hydrogen should flow through the pressurized tube into the target tank")))
              .thenSucceed();
    }

    @GameTest(maxTicks = 600)
    public void digitalMinerMinesBlocks(GameTestHelper helper) {
        BlockPos minerPos = new BlockPos(2, 3, 2);
        BlockPos ore = new BlockPos(2, 1, 2);
        helper.setBlock(ore, Blocks.STONE);
        TileEntityDigitalMiner miner = place(helper, minerPos, MekanismBlocks.DIGITAL_MINER, Direction.NORTH, TileEntityDigitalMiner.class);
        helper.startSequence()
              .thenIdle(2)
              .thenExecute(() -> {
                  int y = helper.absolutePos(ore).getY();
                  miner.setRadiusFromPacket(1);
                  miner.setMinYFromPacket(y);
                  miner.setMaxYFromPacket(y);
                  //No filters in inverse mode mines everything
                  miner.toggleInverse();
                  miner.energyContainer().setEnergy(miner.energyContainer().getCapacityAsLong(), null);
                  miner.start();
              })
              .thenWaitUntil(() -> {
                  miner.energyContainer().setEnergy(miner.energyContainer().getCapacityAsLong(), null);
                  helper.assertBlockPresent(Blocks.AIR, ore);
              })
              .thenExecute(() -> helper.assertTrue(miner.getInventorySlots().stream().anyMatch(slot -> slot.resource().is(Items.COBBLESTONE)),
                    Component.literal("The digital miner should store the mined cobblestone")))
              .thenSucceed();
    }

    @GameTest(maxTicks = 200)
    public void qioImporterStoresItemsInDrives(GameTestHelper helper) {
        BlockPos drivePos = new BlockPos(1, 1, 1);
        BlockPos importerPos = new BlockPos(3, 1, 1);
        //The importer pulls from its back
        BlockPos chestPos = importerPos.south();
        UUID owner = UUID.randomUUID();
        FrequencyIdentity identity = new FrequencyIdentity("fabric_qio_test_" + owner, SecurityMode.PUBLIC, owner);
        TileEntityQIODriveArray driveArray = place(helper, drivePos, MekanismBlocks.QIO_DRIVE_ARRAY, Direction.NORTH, TileEntityQIODriveArray.class);
        TileEntityQIOImporter importer = place(helper, importerPos, MekanismBlocks.QIO_IMPORTER, Direction.NORTH, TileEntityQIOImporter.class);
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.getBlockEntity(chestPos, ChestBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 32));
        driveArray.getDriveSlots().getFirst().setContents(MekanismItems.BASE_QIO_DRIVE.asResource(), 1, null);
        driveArray.getFrequencyComponent().setFrequencyFromData(FrequencyTypes.QIO, identity, owner);
        importer.getFrequencyComponent().setFrequencyFromData(FrequencyTypes.QIO, identity, owner);
        //Note: Placed importers don't import without filters by default (the item's default component)
        if (!importer.getImportWithoutFilter()) {
            importer.toggleImportWithoutFilter();
        }
        ItemResource diamond = ItemResource.of(Items.DIAMOND);
        helper.startSequence()
              .thenWaitUntil(() -> {
                  QIOFrequency frequency = driveArray.getQIOFrequency();
                  helper.assertTrue(frequency != null, Component.literal("The drive array should have a QIO frequency"));
                  helper.assertValueEqual(frequency.getStored(diamond), 32L, "diamonds stored in the QIO frequency");
                  helper.assertTrue(helper.getBlockEntity(chestPos, ChestBlockEntity.class).isEmpty(), Component.literal("The chest should be empty"));
              })
              .thenExecute(() -> {
                  QIOFrequency frequency = driveArray.getQIOFrequency();
                  try (Transaction transaction = Transaction.openRoot()) {
                      helper.assertValueEqual(frequency.massExtract(diamond, 10, transaction), 10L, "extracted diamonds");
                      transaction.commit();
                  }
                  helper.assertValueEqual(frequency.getStored(diamond), 22L, "diamonds left in the QIO frequency");
                  //Clean up the frequency, so it isn't persisted
                  driveArray.getFrequencyComponent().removeFrequencyFromData(FrequencyTypes.QIO, identity, owner);
              })
              .thenSucceed();
    }

    /**
     * Builds a teleporter with a frame above it (3 wide, 4 high).
     */
    private static TileEntityTeleporter teleporter(GameTestHelper helper, BlockPos pos, FrequencyIdentity identity, UUID owner) {
        for (int y = 0; y <= 3; y++) {
            helper.setBlock(pos.offset(-1, y, 0), MekanismBlocks.TELEPORTER_FRAME.defaultState());
            helper.setBlock(pos.offset(1, y, 0), MekanismBlocks.TELEPORTER_FRAME.defaultState());
        }
        helper.setBlock(pos.above(3), MekanismBlocks.TELEPORTER_FRAME.defaultState());
        TileEntityTeleporter teleporter = place(helper, pos, MekanismBlocks.TELEPORTER, Direction.NORTH, TileEntityTeleporter.class);
        teleporter.getFrequencyComponent().setFrequencyFromData(FrequencyTypes.TELEPORTER, identity, owner);
        teleporter.energyContainer().setEnergy(teleporter.energyContainer().getCapacityAsLong(), null);
        return teleporter;
    }

    @GameTest(maxTicks = 200)
    public void teleporterTeleportsEntities(GameTestHelper helper) {
        BlockPos from = new BlockPos(1, 1, 1);
        BlockPos to = new BlockPos(5, 1, 1);
        UUID owner = UUID.randomUUID();
        FrequencyIdentity identity = new FrequencyIdentity("fabric_teleporter_test_" + owner, SecurityMode.PUBLIC, owner);
        TileEntityTeleporter source = teleporter(helper, from, identity, owner);
        teleporter(helper, to, identity, owner);
        helper.startSequence()
              .thenIdle(2)
              .thenExecute(() -> helper.spawn(EntityTypes.PIG, from.above()))
              .thenWaitUntil(() -> {
                  List<Pig> pigs = helper.getEntities(EntityTypes.PIG, to.above(), 1.5);
                  helper.assertTrue(!pigs.isEmpty(), Component.literal("The pig should be teleported to the other teleporter (status: " + source.status + ")"));
              })
              .thenExecute(() -> source.getFrequencyComponent().removeFrequencyFromData(FrequencyTypes.TELEPORTER, identity, owner))
              .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void cardboardBoxPicksUpBlocks(GameTestHelper helper) {
        BlockPos chestPos = new BlockPos(1, 1, 1);
        helper.setBlock(chestPos, Blocks.CHEST);
        helper.getBlockEntity(chestPos, ChestBlockEntity.class).setItem(0, new ItemStack(Items.DIAMOND, 3));
        ServerPlayer player = player(helper, new ItemStack(MekanismBlocks.CARDBOARD_BOX));
        InteractionResult result = useOn(helper, player, chestPos, Direction.UP);
        helper.assertTrue(result.consumesAction(), Component.literal("Using a cardboard box on a chest should succeed (result: " + result + ")"));
        helper.assertBlockPresent(MekanismBlocks.CARDBOARD_BOX.defaultState().getBlock(), chestPos);
        helper.assertTrue(player.getMainHandItem().isEmpty(), Component.literal("The cardboard box should be consumed"));
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void configuratorDismantlesWhileSneaking(GameTestHelper helper) {
        BlockPos machine = new BlockPos(1, 1, 1);
        place(helper, machine, MekanismBlocks.ENRICHMENT_CHAMBER, Direction.NORTH, BlockEntity.class);
        ItemStack configurator = new ItemStack(MekanismItems.CONFIGURATOR.asItem());
        configurator.set(MekanismDataComponents.CONFIGURATOR_MODE, ConfiguratorMode.WRENCH);
        ServerPlayer player = player(helper, configurator);
        player.setShiftKeyDown(true);
        useOn(helper, player, machine, Direction.NORTH);
        helper.assertBlockNotPresent(MekanismBlocks.ENRICHMENT_CHAMBER.defaultState().getBlock(), machine);
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void configuratorRotatesMachines(GameTestHelper helper) {
        BlockPos machine = new BlockPos(1, 1, 1);
        place(helper, machine, MekanismBlocks.ENRICHMENT_CHAMBER, Direction.NORTH, BlockEntity.class);
        ItemStack configurator = new ItemStack(MekanismItems.CONFIGURATOR.asItem());
        configurator.set(MekanismDataComponents.CONFIGURATOR_MODE, ConfiguratorMode.ROTATE);
        useOn(helper, player(helper, configurator), machine, Direction.EAST);
        helper.assertValueEqual(Attribute.getFacing(helper.getBlockState(machine)), Direction.EAST, "facing after rotating");
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void breakingBoundingBlocksRemovesTheMainBlock(GameTestHelper helper) {
        BlockPos minerPos = new BlockPos(2, 1, 2);
        BlockPos boundingPos = minerPos.offset(1, 1, 0);
        ServerPlayer player = player(helper, new ItemStack(MekanismBlocks.DIGITAL_MINER));
        helper.placeAt(player, player.getMainHandItem(), minerPos.below(), Direction.UP);
        helper.assertBlockPresent(MekanismBlocks.DIGITAL_MINER.defaultState().getBlock(), minerPos);
        helper.assertBlockPresent(MekanismBlocks.BOUNDING_BLOCK.value(), boundingPos);
        //Mine a bounding block with a tool that can harvest the miner
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(boundingPos)), Component.literal("The bounding block should be destroyed"));
        helper.assertBlockNotPresent(MekanismBlocks.DIGITAL_MINER.defaultState().getBlock(), minerPos);
        helper.assertBlockNotPresent(MekanismBlocks.BOUNDING_BLOCK.value(), boundingPos);
        helper.assertBlockNotPresent(MekanismBlocks.BOUNDING_BLOCK.value(), minerPos.offset(-1, 1, -1));
        helper.assertTrue(helper.getEntities(EntityTypes.ITEM, minerPos, 3).stream().anyMatch(item -> item.getItem().is(MekanismBlocks.DIGITAL_MINER.asItem())),
              Component.literal("The digital miner should drop itself"));
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void machineDataPersists(GameTestHelper helper) {
        BlockPos cubePos = new BlockPos(1, 1, 1);
        TileEntityEnergyCube cube = place(helper, cubePos, MekanismBlocks.BASIC_ENERGY_CUBE, Direction.NORTH, TileEntityEnergyCube.class);
        cube.energyContainer().setEnergy(12_345, null);
        //Save and load the block entity (chunk saving)
        CompoundTag saved = cube.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(cube.getBlockPos(), cube.getBlockState(), saved, helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof TileEntityEnergyCube, Component.literal("Expected an energy cube to be loaded"));
        helper.assertValueEqual(((TileEntityEnergyCube) loaded).energyContainer().getAmountAsLong(), 12_345L, "energy after loading");
        //Break the cube, and place the dropped item again (item data components)
        helper.getLevel().destroyBlock(helper.absolutePos(cubePos), true);
        List<ItemEntity> drops = helper.getEntities(EntityTypes.ITEM, cubePos, 2);
        helper.assertTrue(drops.size() == 1, Component.literal("Expected the energy cube to drop itself, got " + drops.size() + " drops"));
        ItemStack dropped = drops.getFirst().getItem();
        helper.assertTrue(dropped.is(MekanismBlocks.BASIC_ENERGY_CUBE.asItem()), Component.literal("Expected an energy cube item, got " + dropped));
        drops.getFirst().discard();
        helper.placeAt(player(helper, dropped), dropped, cubePos.below(), Direction.UP);
        helper.assertValueEqual(helper.getBlockEntity(cubePos, TileEntityEnergyCube.class).energyContainer().getAmountAsLong(), 12_345L,
              "energy after placing the dropped energy cube");
        helper.succeed();
    }

    @GameTest
    public void configsAreLoaded(GameTestHelper helper) {
        for (IMekanismConfig config : MekanismConfig.getConfigs()) {
            if (config.getConfigType() != MekanismConfigType.CLIENT) {
                helper.assertTrue(config.isLoaded(), Component.literal("Expected Mekanism's " + config.getFileName() + " config to be loaded"));
            }
        }
        helper.assertValueEqual(MekanismConfig.general.minerTicksPerMine.get(), 80, "miner ticks per mine (default value)");
        helper.succeed();
    }
}
