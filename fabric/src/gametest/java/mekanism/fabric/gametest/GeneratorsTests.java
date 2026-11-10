package mekanism.fabric.gametest;

import java.util.function.BiConsumer;
import mekanism.api.AutomationType;
import mekanism.api.MekanismRegistries;
import mekanism.api.chemical.ChemicalIds;
import mekanism.api.chemical.ChemicalResource;
import mekanism.common.command.builders.StructureBuilder;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.generators.common.registries.GeneratorsBlocks;
import mekanism.generators.common.registries.GeneratorsBuilders;
import mekanism.generators.common.tile.TileEntityGasGenerator;
import mekanism.generators.common.tile.TileEntityGenerator;
import mekanism.common.registration.impl.BlockRegistryObject;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Tests that Mekanism: Generators works on Fabric: generators produce energy and its multiblocks form.
 */
public class GeneratorsTests {

    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private static <TILE extends TileEntityGenerator> TILE placeGenerator(GameTestHelper helper, BlockRegistryObject<?, ?> block, Class<TILE> tileClass) {
        helper.setBlock(POS, block.defaultState());
        //Generators get their default configuration from the item they are placed with
        helper.getLevel().getBlockEntity(helper.absolutePos(POS)).applyComponentsFromItemStack(new ItemStack(block));
        return helper.getBlockEntity(POS, tileClass);
    }

    @GameTest(maxTicks = 200)
    public void heatGeneratorProducesEnergyFromLava(GameTestHelper helper) {
        TileEntityGenerator generator = placeGenerator(helper, GeneratorsBlocks.HEAT_GENERATOR, TileEntityGenerator.class);
        //Mekanism's tiles finish initializing when they get loaded (on the next block entity tick)
        helper.runAfterDelay(2, () -> {
            ResourceHandler<FluidResource> fluids = Capabilities.Fluid.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(POS), null, null, Direction.UP);
            helper.assertTrue(fluids != null, Component.literal("Expected the heat generator to expose a fluid handler"));
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertValueEqual(fluids.insert(FluidResource.of(Fluids.LAVA), 1_000, transaction), 1_000, "inserted lava");
                transaction.commit();
            }
            helper.succeedWhen(() -> helper.assertTrue(generator.energyContainer().getAmountAsLong() > 0,
                  Component.literal("The heat generator should produce energy from lava")));
        });
    }

    @GameTest(maxTicks = 200)
    public void gasBurningGeneratorProducesEnergyFromHydrogen(GameTestHelper helper) {
        TileEntityGasGenerator generator = placeGenerator(helper, GeneratorsBlocks.GAS_BURNING_GENERATOR, TileEntityGasGenerator.class);
        helper.runAfterDelay(2, () -> {
            ChemicalResource hydrogen = ChemicalResource.of(helper.getLevel().registryAccess().lookupOrThrow(MekanismRegistries.Keys.CHEMICAL).getOrThrow(ChemicalIds.HYDROGEN));
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertValueEqual(generator.getFuelTank().insert(hydrogen, 1_000, transaction, AutomationType.EXTERNAL), 1_000, "inserted hydrogen");
                transaction.commit();
            }
            helper.succeedWhen(() -> helper.assertTrue(generator.energyContainer().getAmountAsLong() > 0,
                  Component.literal("The gas burning generator should produce energy from hydrogen")));
        });
    }

    /**
     * Builds a structure (with one of Mekanism's {@link StructureBuilder build command builders}) and waits for the multiblock to form.
     */
    private static void assertForms(GameTestHelper helper, BiConsumer<Level, BlockPos> builder, BlockPos start, BlockPos tilePos, String name) {
        builder.accept(helper.getLevel(), helper.absolutePos(start));
        helper.succeedWhen(() -> {
            if (!(helper.getLevel().getBlockEntity(helper.absolutePos(tilePos)) instanceof TileEntityMultiblock<?> tile)) {
                throw helper.assertionException(tilePos, "Expected a multiblock tile");
            }
            MultiblockData multiblock = tile.getMultiblock();
            helper.assertTrue(multiblock.isFormed(), Component.literal("The " + name + " should form"));
        });
    }

    @GameTest(maxTicks = 200)
    public void fusionReactorForms(GameTestHelper helper) {
        //Note: The builders' build method is protected, so call it from anonymous subclasses
        assertForms(helper, (level, start) -> new GeneratorsBuilders.FusionReactorBuilder() {{
            build(level, start, false);
        }}, new BlockPos(1, 1, 1), new BlockPos(3, 5, 3), "fusion reactor");
    }

    @GameTest(maxTicks = 200)
    public void industrialTurbineForms(GameTestHelper helper) {
        assertForms(helper, (level, start) -> new GeneratorsBuilders.TurbineBuilder(5, 5) {{
            build(level, start, false);
        }}, new BlockPos(1, 1, 1), new BlockPos(1, 1, 1), "industrial turbine");
    }

    @GameTest(maxTicks = 200)
    public void fissionReactorForms(GameTestHelper helper) {
        assertForms(helper, (level, start) -> new SmallFissionReactorBuilder().build(level, start, false), new BlockPos(1, 1, 1), new BlockPos(1, 1, 1),
              "fission reactor");
    }

    /**
     * A 5x5x5 fission reactor with one fuel assembly (the build command's one is 18x18x18, which doesn't fit in a test structure).
     */
    private static class SmallFissionReactorBuilder extends StructureBuilder {

        private SmallFissionReactorBuilder() {
            super(5, 5, 5);
        }

        @Override
        protected void build(Level world, BlockPos start, boolean empty) {
            buildFrame(world, start);
            buildWalls(world, start);
            buildInteriorLayers(world, start, 1, 3, Blocks.AIR.defaultBlockState());
            world.setBlockAndUpdate(start.offset(2, 1, 2), GeneratorsBlocks.FISSION_FUEL_ASSEMBLY.defaultState());
            world.setBlockAndUpdate(start.offset(2, 2, 2), GeneratorsBlocks.FISSION_FUEL_ASSEMBLY.defaultState());
            world.setBlockAndUpdate(start.offset(2, 3, 2), GeneratorsBlocks.CONTROL_ROD_ASSEMBLY.defaultState());
        }

        @Override
        protected BlockState getWallBlock(BlockPos pos) {
            return GeneratorsBlocks.REACTOR_GLASS.defaultState();
        }

        @Override
        protected BlockState getCasing() {
            return GeneratorsBlocks.FISSION_REACTOR_CASING.defaultState();
        }
    }
}
