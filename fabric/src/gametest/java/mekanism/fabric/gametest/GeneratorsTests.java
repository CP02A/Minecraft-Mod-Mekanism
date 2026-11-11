package mekanism.fabric.gametest;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import mekanism.common.lib.radiation.Meltdown;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.phys.Vec3;
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

    private static FissionReactorMultiblockData formedReactor(GameTestHelper helper) {
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1))) instanceof TileEntityMultiblock<?> tile)
            || !(tile.getMultiblock() instanceof FissionReactorMultiblockData reactor) || !reactor.isFormed()) {
            throw helper.assertionException(new BlockPos(1, 1, 1), "Expected a formed fission reactor");
        }
        return reactor;
    }

    /**
     * Mobs path around the casing of a formed multiblock like around a fence (IBlockExtension#getBlockPathType), so they don't walk into it; vanilla
     * would treat the solid casing as blocked.
     */
    @GameTest(maxTicks = 200)
    public void formedMultiblockCasingIsAFenceForPathfinding(GameTestHelper helper) {
        new SmallFissionReactorBuilder().build(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), false);
        helper.startSequence()
              .thenWaitUntil(() -> formedReactor(helper))
              .thenExecute(() -> {
                  Zombie zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(7, 1, 7));
                  //A floor casing with the reactor's inside above it
                  BlockPos casing = helper.absolutePos(new BlockPos(2, 1, 2));
                  helper.assertValueEqual(new PathfindingContext(helper.getLevel(), zombie).getPathTypeFromState(casing.getX(), casing.getY(), casing.getZ()),
                        PathType.FENCE, "path type of the formed reactor's casing");
                  zombie.discard();
              })
              .thenSucceed();
    }

    /**
     * A meltdown explosion makes the reactor melt down when it destroys one of its blocks (IBlockExtension#onBlockExploded), which damages the reactor.
     */
    @GameTest(maxTicks = 200)
    public void meltdownExplosionMeltsTheReactorDown(GameTestHelper helper) {
        new SmallFissionReactorBuilder().build(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), false);
        helper.startSequence()
              .thenWaitUntil(() -> formedReactor(helper))
              .thenExecute(() -> {
                  FissionReactorMultiblockData reactor = formedReactor(helper);
                  helper.assertValueEqual(reactor.reactorDamage, FissionReactorMultiblockData.DEFAULT_REACTOR_DAMAGE, "reactor damage before the meltdown");
                  Vec3 center = Vec3.atCenterOf(reactor.getBounds().getCenter());
                  Meltdown meltdown = new Meltdown(reactor.getBounds(), 1, 1, 1, reactor.inventoryID);
                  try {
                      //The meltdown's explosions are random; the explosion itself always destroys one of the reactor's walls
                      Method createExplosion = Meltdown.class.getDeclaredMethod("createExplosion", ServerLevel.class, double.class, double.class, double.class,
                            float.class, boolean.class, Explosion.BlockInteraction.class);
                      createExplosion.setAccessible(true);
                      createExplosion.invoke(meltdown, helper.getLevel(), center.x, center.y, center.z, 1F, false, Explosion.BlockInteraction.DESTROY);
                  } catch (ReflectiveOperationException e) {
                      throw new RuntimeException(e);
                  }
                  helper.assertTrue(reactor.reactorDamage > FissionReactorMultiblockData.DEFAULT_REACTOR_DAMAGE,
                        Component.literal("Expected the meltdown to damage the reactor, its damage is " + reactor.reactorDamage));
              })
              .thenSucceed();
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
