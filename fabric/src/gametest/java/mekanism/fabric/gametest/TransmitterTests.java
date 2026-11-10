package mekanism.fabric.gametest;

import mekanism.common.block.attribute.Attribute;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Checks that Mekanism's transmitters connect to blocks that expose the matching capability (resolved through {@code BlockCapabilityCache}s, which
 * rely on capability invalidation when block entities are placed or removed), and that energy flows through universal cables.
 */
public class TransmitterTests {

    /**
     * Places a block like a player would (applying the item's default components, such as the default side configuration of machines and energy
     * cubes).
     */
    private static void place(GameTestHelper helper, BlockPos pos, BlockRegistryObject<?, ?> block, Direction facing) {
        BlockState state = Attribute.setFacing(block.defaultState(), facing);
        helper.setBlock(pos, state == null ? block.defaultState() : state);
        BlockEntity blockEntity = helper.getBlockEntity(pos, BlockEntity.class);
        blockEntity.applyComponentsFromItemStack(new ItemStack(block));
    }

    private static Transmitter<?, ?, ?> transmitter(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, TileEntityTransmitter.class).getTransmitter();
    }

    private static void assertAcceptor(GameTestHelper helper, BlockPos pos, Direction side, boolean connected) {
        byte acceptors = transmitter(helper, pos).getAcceptorCache().currentAcceptorConnections;
        if (Transmitter.connectionMapContainsSide(acceptors, side) != connected) {
            helper.fail(Component.literal("Expected the transmitter to " + (connected ? "" : "not ") + "connect to the acceptor on its " + side +
                                          " side (acceptor connections: " + Integer.toBinaryString(acceptors) + ")"), pos);
        }
    }

    private static void assertTransmitter(GameTestHelper helper, BlockPos pos, Direction side) {
        if (!Transmitter.connectionMapContainsSide(transmitter(helper, pos).currentTransmitterConnections, side)) {
            helper.fail(Component.literal("Expected the transmitter to connect to the transmitter on its " + side + " side"), pos);
        }
    }

    @GameTest(maxTicks = 200)
    public void universalCablesConnectToEnergyCubesAndMachines(GameTestHelper helper) {
        BlockPos source = new BlockPos(1, 1, 1);
        BlockPos cable1 = source.south();
        BlockPos cable2 = cable1.south();
        BlockPos target = cable2.south();
        BlockPos machine = cable2.east();
        //The source outputs to the cable through its front, the target accepts energy through its back
        place(helper, source, MekanismBlocks.BASIC_ENERGY_CUBE, Direction.SOUTH);
        helper.setBlock(cable1, MekanismBlocks.BASIC_UNIVERSAL_CABLE.defaultState());
        helper.setBlock(cable2, MekanismBlocks.BASIC_UNIVERSAL_CABLE.defaultState());
        helper.startSequence()
              .thenIdle(2)
              //Placed after the cables loaded, so the cables only find them if their capability caches get invalidated
              .thenExecute(() -> {
                  place(helper, target, MekanismBlocks.BASIC_ENERGY_CUBE, Direction.SOUTH);
                  place(helper, machine, MekanismBlocks.ENRICHMENT_CHAMBER, Direction.NORTH);
              })
              .thenIdle(5)
              .thenExecute(() -> {
                  assertAcceptor(helper, cable1, Direction.NORTH, true);
                  assertTransmitter(helper, cable1, Direction.SOUTH);
                  assertAcceptor(helper, cable2, Direction.SOUTH, true);
                  assertAcceptor(helper, cable2, Direction.EAST, true);
                  //Charge the source, which then ejects through the cables
                  helper.getBlockEntity(source, TileEntityEnergyCube.class).energyContainer().setEnergy(1_000_000, null);
              })
              .thenWaitUntil(() -> helper.assertTrue(helper.getBlockEntity(target, TileEntityEnergyCube.class).energyContainer().getAmountAsLong() > 0,
                    Component.literal("Energy should flow through the cables into the target energy cube")))
              //Removing an acceptor has to disconnect it
              .thenExecute(() -> helper.setBlock(machine, Blocks.AIR))
              .thenIdle(5)
              .thenExecute(() -> assertAcceptor(helper, cable2, Direction.EAST, false))
              .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void logisticalTransportersConnectToMachinesAndContainers(GameTestHelper helper) {
        BlockPos transporter = new BlockPos(1, 1, 2);
        BlockPos machine = transporter.north();
        BlockPos chest = transporter.south();
        helper.setBlock(transporter, MekanismBlocks.BASIC_LOGISTICAL_TRANSPORTER.defaultState());
        helper.startSequence()
              .thenIdle(2)
              .thenExecute(() -> {
                  place(helper, machine, MekanismBlocks.ENRICHMENT_CHAMBER, Direction.NORTH);
                  //A vanilla chest (exposed through Fabric's item storage) doesn't invalidate capabilities itself
                  helper.setBlock(chest, Blocks.CHEST);
              })
              .thenIdle(5)
              .thenExecute(() -> {
                  assertAcceptor(helper, transporter, Direction.NORTH, true);
                  assertAcceptor(helper, transporter, Direction.SOUTH, true);
                  helper.setBlock(chest, Blocks.AIR);
              })
              .thenIdle(5)
              .thenExecute(() -> assertAcceptor(helper, transporter, Direction.SOUTH, false))
              .thenSucceed();
    }

    @GameTest(maxTicks = 100)
    public void mechanicalPipesConnectToTanksAndMachines(GameTestHelper helper) {
        BlockPos pipe = new BlockPos(1, 2, 2);
        BlockPos tank = pipe.north();
        //The electric pump only exposes its fluid tank on its top
        BlockPos pump = pipe.below();
        helper.setBlock(pipe, MekanismBlocks.BASIC_MECHANICAL_PIPE.defaultState());
        helper.startSequence()
              .thenIdle(2)
              .thenExecute(() -> {
                  place(helper, tank, MekanismBlocks.BASIC_FLUID_TANK, Direction.NORTH);
                  place(helper, pump, MekanismBlocks.ELECTRIC_PUMP, Direction.NORTH);
              })
              .thenIdle(5)
              .thenExecute(() -> {
                  assertAcceptor(helper, pipe, Direction.NORTH, true);
                  assertAcceptor(helper, pipe, Direction.DOWN, true);
              })
              .thenSucceed();
    }
}
