package mekanism.fabric.gametest;

import java.util.List;
import java.util.Map;
import mekanism.api.AutomationType;
import mekanism.api.MekanismRegistries;
import mekanism.api.chemical.ChemicalIds;
import mekanism.api.chemical.ChemicalResource;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.command.builders.StructureBuilder;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityChemicalTank;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.TileEntityFluidTank;
import mekanism.common.tile.interfaces.IRedstoneControl.RedstoneControl;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import mekanism.common.tile.machine.TileEntityEnrichmentChamber;
import mekanism.common.tile.multiblock.TileEntityInductionPort;
import mekanism.generators.common.registries.GeneratorsBlocks;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorLogicAdapter;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Tests the CC: Tweaked integration (CC: Tweaked is only in the game test server run): Mekanism's peripherals are found through CC's Fabric
 * {@code PeripheralLookup}, their methods can be called and convert Mekanism's types to Lua values, and a computer running a Lua script uses them. All
 * CC: Tweaked specific code is in {@link ComputerCraftAccess}.
 */
public class ComputerCraftTests {

    private static final BlockPos POS = new BlockPos(2, 1, 2);

    /**
     * Places a block like a player would (applying the item's default components, such as the default side configuration).
     */
    private static <BE extends BlockEntity> BE place(GameTestHelper helper, BlockPos pos, BlockRegistryObject<?, ?> block, Class<BE> beClass) {
        BlockState state = Attribute.setFacing(block.defaultState(), Direction.NORTH);
        helper.setBlock(pos, state == null ? block.defaultState() : state);
        BE blockEntity = helper.getBlockEntity(pos, beClass);
        blockEntity.applyComponentsFromItemStack(new ItemStack(block));
        return blockEntity;
    }

    private static Object peripheral(GameTestHelper helper, BlockPos pos, String expectedType) {
        Object peripheral = ComputerCraftAccess.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
        if (peripheral == null) {
            throw helper.assertionException(pos, "Expected a " + expectedType + " peripheral");
        }
        helper.assertValueEqual(ComputerCraftAccess.type(peripheral), expectedType, "peripheral type");
        return peripheral;
    }

    private static Object callSingle(Object peripheral, String method, Object... arguments) {
        Object[] results = ComputerCraftAccess.call(peripheral, method, arguments);
        return results.length == 0 ? null : results[0];
    }

    private static long number(GameTestHelper helper, Object value, String name) {
        if (!(value instanceof Number number)) {
            throw helper.assertionException(name + " should be a number, but is " + value);
        }
        return number.longValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> table(GameTestHelper helper, @Nullable Object value, String name) {
        if (!(value instanceof Map<?, ?> map)) {
            throw helper.assertionException(name + " should be a table, but is " + value);
        }
        return (Map<String, Object>) map;
    }

    @GameTest
    public void energyCubePeripheral(GameTestHelper helper) {
        TileEntityEnergyCube cube = place(helper, POS, MekanismBlocks.BASIC_ENERGY_CUBE, TileEntityEnergyCube.class);
        helper.runAfterDelay(2, () -> {
            cube.energyContainer().setEnergy(1_000, null);
            Object peripheral = peripheral(helper, POS, "basicEnergyCube");
            helper.assertTrue(ComputerCraftAccess.target(peripheral) == cube, Component.literal("The peripheral's target should be the energy cube"));
            List<String> methods = ComputerCraftAccess.methods(peripheral);
            for (String method : List.of("getEnergy", "getMaxEnergy", "getEnergyNeeded", "getRedstoneMode", "setRedstoneMode", "getChargeItem", "help")) {
                helper.assertTrue(methods.contains(method), Component.literal("The energy cube peripheral should have " + method + ", it has " + methods));
            }
            helper.assertValueEqual(number(helper, callSingle(peripheral, "getEnergy"), "getEnergy"), 1_000L, "getEnergy");
            helper.assertValueEqual(number(helper, callSingle(peripheral, "getMaxEnergy"), "getMaxEnergy"), cube.energyContainer().getCapacityAsLong(), "getMaxEnergy");
            helper.assertValueEqual(callSingle(peripheral, "getRedstoneMode"), cube.getControlType().name(), "getRedstoneMode");
            //Setters change the tile (and are run on the main thread)
            ComputerCraftAccess.call(peripheral, "setRedstoneMode", "HIGH");
            helper.assertValueEqual(cube.getControlType(), RedstoneControl.HIGH, "redstone mode after setRedstoneMode");
            helper.assertValueEqual(callSingle(peripheral, "getRedstoneMode"), "HIGH", "getRedstoneMode after setting it");
            //Invalid arguments and argument counts are Lua errors
            assertLuaError(helper, () -> ComputerCraftAccess.call(peripheral, "setRedstoneMode", "NOT_A_MODE"), "an invalid redstone mode");
            assertLuaError(helper, () -> ComputerCraftAccess.call(peripheral, "setRedstoneMode"), "a missing argument");
            //The item slots are converted to tables
            Map<String, Object> chargeItem = table(helper, callSingle(peripheral, "getChargeItem"), "getChargeItem");
            helper.assertValueEqual(chargeItem.get("name"), "minecraft:air", "name of the empty charge item");
            //CC finds the same peripheral each time (it compares them to detect changes)
            helper.assertTrue(ComputerCraftAccess.isEqual(peripheral, peripheral(helper, POS, "basicEnergyCube")), Component.literal("The peripheral should be equal when looked up again"));
            helper.succeed();
        });
    }

    private static void assertLuaError(GameTestHelper helper, Runnable call, String what) {
        try {
            call.run();
        } catch (IllegalStateException e) {
            return;
        }
        throw helper.assertionException("Calling a method with " + what + " should be a Lua error");
    }

    @GameTest
    public void enrichmentChamberPeripheral(GameTestHelper helper) {
        TileEntityEnrichmentChamber chamber = place(helper, POS, MekanismBlocks.ENRICHMENT_CHAMBER, TileEntityEnrichmentChamber.class);
        helper.runAfterDelay(2, () -> {
            Object peripheral = peripheral(helper, POS, "enrichmentChamber");
            helper.assertValueEqual(number(helper, callSingle(peripheral, "getEnergy"), "getEnergy"), 0L, "getEnergy");
            helper.assertValueEqual(number(helper, callSingle(peripheral, "getMaxEnergy"), "getMaxEnergy"), chamber.energyContainer().getCapacityAsLong(), "getMaxEnergy");
            helper.assertValueEqual(callSingle(peripheral, "getRedstoneMode"), "DISABLED", "getRedstoneMode");
            ComputerCraftAccess.call(peripheral, "setRedstoneMode", "HIGH");
            helper.assertValueEqual(chamber.getControlType(), RedstoneControl.HIGH, "redstone mode after setRedstoneMode");
            //Slot contents are converted to tables with their name and amount
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertValueEqual(chamber.getInventorySlots().getFirst().insert(ItemResource.of(Items.IRON_ORE), 3, transaction, AutomationType.INTERNAL), 3, "inserted iron ore");
                transaction.commit();
            }
            Map<String, Object> input = table(helper, callSingle(peripheral, "getInput"), "getInput");
            helper.assertValueEqual(input.get("name"), "minecraft:iron_ore", "name of the input item");
            helper.assertValueEqual(number(helper, input.get("amount"), "amount"), 3L, "amount of the input item");
            helper.succeed();
        });
    }

    @GameTest
    public void tanksConvertFluidsAndChemicals(GameTestHelper helper) {
        BlockPos chemicalPos = POS.east();
        TileEntityFluidTank fluidTank = place(helper, POS, MekanismBlocks.BASIC_FLUID_TANK, TileEntityFluidTank.class);
        TileEntityChemicalTank chemicalTank = place(helper, chemicalPos, MekanismBlocks.BASIC_CHEMICAL_TANK, TileEntityChemicalTank.class);
        helper.runAfterDelay(2, () -> {
            ChemicalResource hydrogen = ChemicalResource.of(helper.getLevel().registryAccess().lookupOrThrow(MekanismRegistries.Keys.CHEMICAL).getOrThrow(ChemicalIds.HYDROGEN));
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertValueEqual(fluidTank.fluidTank.insert(FluidResource.of(Fluids.WATER), 1_000, transaction, AutomationType.INTERNAL), 1_000, "inserted water");
                helper.assertValueEqual(chemicalTank.getChemicalTank().insert(hydrogen, 500, transaction, AutomationType.INTERNAL), 500, "inserted hydrogen");
                transaction.commit();
            }
            Map<String, Object> fluid = table(helper, callSingle(peripheral(helper, POS, "basicFluidTank"), "getStored"), "fluid tank getStored");
            helper.assertValueEqual(fluid.get("name"), "minecraft:water", "name of the stored fluid");
            helper.assertValueEqual(number(helper, fluid.get("amount"), "amount"), 1_000L, "amount of the stored fluid");
            Map<String, Object> chemical = table(helper, callSingle(peripheral(helper, chemicalPos, "basicChemicalTank"), "getStored"), "chemical tank getStored");
            helper.assertValueEqual(chemical.get("name"), ChemicalIds.HYDROGEN.identifier().toString(), "name of the stored chemical");
            helper.assertValueEqual(number(helper, chemical.get("amount"), "amount"), 500L, "amount of the stored chemical");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 200)
    public void inductionPortExposesTheMatrix(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 1, 1);
        BlockPos port = start.offset(1, 1, 0);
        new CuboidBuilder(4, MekanismBlocks.INDUCTION_CASING.defaultState(), MekanismBlocks.STRUCTURAL_GLASS.defaultState(), (level, s) -> {
            for (int x = 1; x <= 2; x++) {
                for (int z = 1; z <= 2; z++) {
                    level.setBlockAndUpdate(s.offset(x, 1, z), MekanismBlocks.BASIC_INDUCTION_CELL.defaultState());
                    level.setBlockAndUpdate(s.offset(x, 2, z), MekanismBlocks.BASIC_INDUCTION_PROVIDER.defaultState());
                }
            }
            level.setBlockAndUpdate(s.offset(1, 1, 0), MekanismBlocks.INDUCTION_PORT.defaultState());
        }).build(helper.getLevel(), helper.absolutePos(start), false);
        helper.startSequence()
              .thenWaitUntil(() -> helper.assertTrue(helper.getBlockEntity(port, TileEntityInductionPort.class).getMultiblock().isFormed(),
                    Component.literal("The induction matrix should form")))
              .thenExecute(() -> {
                  Object peripheral = peripheral(helper, port, "inductionPort");
                  helper.assertValueEqual(callSingle(peripheral, "isFormed"), true, "isFormed");
                  helper.assertValueEqual(number(helper, callSingle(peripheral, "getInstalledCells"), "getInstalledCells"), 4L, "getInstalledCells");
                  helper.assertValueEqual(number(helper, callSingle(peripheral, "getInstalledProviders"), "getInstalledProviders"), 4L, "getInstalledProviders");
                  helper.assertTrue(number(helper, callSingle(peripheral, "getMaxEnergy"), "getMaxEnergy") > 0, Component.literal("The matrix should have a capacity"));
              })
              .thenSucceed();
    }

    @GameTest(maxTicks = 200)
    public void fissionReactorLogicAdapterPeripheral(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 1, 1);
        BlockPos adapter = start.offset(1, 2, 0);
        new CuboidBuilder(5, GeneratorsBlocks.FISSION_REACTOR_CASING.defaultState(), GeneratorsBlocks.REACTOR_GLASS.defaultState(), (level, s) -> {
            level.setBlockAndUpdate(s.offset(2, 1, 2), GeneratorsBlocks.FISSION_FUEL_ASSEMBLY.defaultState());
            level.setBlockAndUpdate(s.offset(2, 2, 2), GeneratorsBlocks.FISSION_FUEL_ASSEMBLY.defaultState());
            level.setBlockAndUpdate(s.offset(2, 3, 2), GeneratorsBlocks.CONTROL_ROD_ASSEMBLY.defaultState());
            level.setBlockAndUpdate(s.offset(1, 2, 0), GeneratorsBlocks.FISSION_REACTOR_LOGIC_ADAPTER.defaultState());
        }).build(helper.getLevel(), helper.absolutePos(start), false);
        helper.startSequence()
              .thenWaitUntil(() -> helper.assertTrue(helper.getBlockEntity(adapter, TileEntityFissionReactorLogicAdapter.class).getMultiblock().isFormed(),
                    Component.literal("The fission reactor should form")))
              .thenExecute(() -> {
                  Object peripheral = peripheral(helper, adapter, "fissionReactorLogicAdapter");
                  helper.assertValueEqual(callSingle(peripheral, "isFormed"), true, "isFormed");
                  helper.assertValueEqual(callSingle(peripheral, "getLogicMode"), "DISABLED", "getLogicMode");
                  helper.assertValueEqual(callSingle(peripheral, "getStatus"), false, "getStatus (the reactor is off)");
                  Map<String, Object> fuel = table(helper, callSingle(peripheral, "getFuel"), "getFuel");
                  helper.assertValueEqual(number(helper, fuel.get("amount"), "amount"), 0L, "amount of fuel");
              })
              .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void boundingBlocksProxyThePeripheral(GameTestHelper helper) {
        BlockPos minerPos = new BlockPos(2, 1, 2);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MekanismBlocks.DIGITAL_MINER));
        helper.placeAt(player, player.getMainHandItem(), minerPos.below(), Direction.UP);
        helper.runAfterDelay(2, () -> {
            TileEntityDigitalMiner miner = helper.getBlockEntity(minerPos, TileEntityDigitalMiner.class);
            int proxied = 0;
            for (BlockPos pos : BlockPos.betweenClosed(minerPos.offset(-1, 0, -1), minerPos.offset(1, 2, 1))) {
                if (!pos.equals(minerPos) && helper.getBlockState(pos).is(MekanismBlocks.BOUNDING_BLOCK.value())) {
                    Object peripheral = ComputerCraftAccess.find(helper.getLevel(), helper.absolutePos(pos), Direction.UP);
                    if (peripheral != null) {
                        helper.assertValueEqual(ComputerCraftAccess.type(peripheral), "digitalMiner", "type of the proxied peripheral");
                        helper.assertTrue(ComputerCraftAccess.target(peripheral) == miner, Component.literal("The proxied peripheral's target should be the miner"));
                        proxied++;
                    }
                }
            }
            helper.assertTrue(proxied > 0, Component.literal("At least one of the digital miner's bounding blocks should proxy its peripheral"));
            helper.succeed();
        });
    }

    /**
     * A computer running a Lua script (the test's ROM autorun script {@code mekanism_fabric_tests.lua}) next to an energy cube. The computer boots on CC's
     * own threads, which can take a few seconds of real time while the game test server may tick faster than 20 TPS, hence the generous tick limit.
     */
    @GameTest(maxTicks = 2400)
    public void luaScriptUsesTheEnergyCube(GameTestHelper helper) {
        TileEntityEnergyCube cube = place(helper, POS, MekanismBlocks.BASIC_ENERGY_CUBE, TileEntityEnergyCube.class);
        BlockPos computerPos = POS.above();
        helper.runAfterDelay(2, () -> {
            cube.energyContainer().setEnergy(1_000, null);
            ComputerCraftAccess.startComputer(helper.getLevel(), helper.absolutePos(computerPos), "mekanism_test");
            helper.succeedWhen(() -> {
                String label = ComputerCraftAccess.label(helper.getLevel(), helper.absolutePos(computerPos));
                if ("mekanism_test".equals(label) || label == null) {
                    throw helper.assertionException(computerPos, "The computer's script hasn't finished");
                }
                helper.assertValueEqual(label, "ok", "result of the Lua script");
                helper.assertValueEqual(cube.getControlType(), RedstoneControl.LOW, "redstone mode set by the Lua script");
            });
        });
    }

    /**
     * A cuboid multiblock of the given size (the build command's builders are 18x18x18), with extra blocks placed by a callback.
     */
    private static class CuboidBuilder extends StructureBuilder {

        private final BlockState casing;
        private final BlockState wall;
        private final java.util.function.BiConsumer<Level, BlockPos> extras;

        private CuboidBuilder(int size, BlockState casing, BlockState wall, java.util.function.BiConsumer<Level, BlockPos> extras) {
            super(size, size, size);
            this.casing = casing;
            this.wall = wall;
            this.extras = extras;
        }

        @Override
        protected void build(Level world, BlockPos start, boolean empty) {
            buildFrame(world, start);
            buildWalls(world, start);
            buildInteriorLayers(world, start, 1, sizeY - 2, Blocks.AIR.defaultBlockState());
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
    }
}
