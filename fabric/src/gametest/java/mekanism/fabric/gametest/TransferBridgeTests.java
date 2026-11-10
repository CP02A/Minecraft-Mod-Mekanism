package mekanism.fabric.gametest;

import mekanism.common.registries.MekanismBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Tests the bridge between NeoForge's transfer capabilities (as used by Mekanism) and Fabric's transfer API.
 */
public class TransferBridgeTests {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static ResourceHandler<FluidResource> neoFluidHandler(GameTestHelper helper, BlockPos pos) {
        ResourceHandler<FluidResource> handler = Capabilities.Fluid.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(pos), null, null, Direction.UP);
        if (handler == null) {
            throw helper.assertionException("Expected a NeoForge fluid handler at %s", pos);
        }
        return handler;
    }

    @GameTest
    public void fabricCanFillMekanismTank(GameTestHelper helper) {
        helper.setBlock(POS, MekanismBlocks.BASIC_FLUID_TANK.defaultState());
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(POS), Direction.UP);
        helper.assertTrue(storage != null, net.minecraft.network.chat.Component.literal("Mekanism's fluid tank should expose a Fabric fluid storage"));
        try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            long inserted = storage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction);
            helper.assertValueEqual(inserted, FluidConstants.BUCKET, "inserted droplets");
            transaction.commit();
        }
        ResourceHandler<FluidResource> handler = neoFluidHandler(helper, POS);
        helper.assertValueEqual(handler.getAmountAsLong(0), 1_000L, "stored millibuckets");
        helper.assertTrue(handler.getResource(0).getFluid() == Fluids.WATER, net.minecraft.network.chat.Component.literal("The tank should contain water"));
        helper.succeed();
    }

    @GameTest
    public void abortedFabricTransactionRollsBackMekanismTank(GameTestHelper helper) {
        helper.setBlock(POS, MekanismBlocks.BASIC_FLUID_TANK.defaultState());
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(POS), Direction.UP);
        helper.assertTrue(storage != null, net.minecraft.network.chat.Component.literal("Mekanism's fluid tank should expose a Fabric fluid storage"));
        try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            storage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction);
            //Not committed
        }
        helper.assertValueEqual(neoFluidHandler(helper, POS).getAmountAsLong(0), 0L, "stored millibuckets after the aborted transaction");
        helper.succeed();
    }

    @GameTest
    public void mekanismCanDrainFabricOnlyCauldron(GameTestHelper helper) {
        //NeoForge doesn't provide a fluid capability for cauldrons, but Fabric does
        helper.setBlock(POS, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        ResourceHandler<FluidResource> handler = neoFluidHandler(helper, POS);
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(FluidResource.of(Fluids.WATER), 1_000, transaction);
            helper.assertValueEqual(extracted, 1_000, "extracted millibuckets");
            transaction.commit();
        }
        helper.assertBlockPresent(Blocks.CAULDRON, POS);
        helper.succeed();
    }

    @GameTest
    public void mekanismCanInsertIntoVanillaChest(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.CHEST);
        ResourceHandler<ItemResource> handler = Capabilities.Item.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(POS), null, null, Direction.UP);
        helper.assertTrue(handler != null, net.minecraft.network.chat.Component.literal("Expected a NeoForge item handler for the chest"));
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(ItemResource.of(Items.DIAMOND), 5, transaction), 5, "inserted diamonds");
            transaction.commit();
        }
        ChestBlockEntity chest = helper.getBlockEntity(POS, ChestBlockEntity.class);
        helper.assertValueEqual(chest.countItem(Items.DIAMOND), 5, "diamonds in the chest");
        helper.succeed();
    }

    @GameTest
    public void fabricCanInsertIntoMekanismMachine(GameTestHelper helper) {
        helper.setBlock(POS, MekanismBlocks.ENRICHMENT_CHAMBER.defaultState());
        //Machines get their default side configuration from the item they are placed with
        helper.getLevel().getBlockEntity(helper.absolutePos(POS)).applyComponentsFromItemStack(new ItemStack(MekanismBlocks.ENRICHMENT_CHAMBER));
        //Mekanism's tiles finish initializing when they get loaded (on the next block entity tick)
        helper.runAfterDelay(2, () -> {
            long inserted = 0;
            for (Direction side : Direction.values()) {
                Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(POS), side);
                if (storage != null) {
                    try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                        inserted = storage.insert(ItemVariant.of(Items.RAW_IRON), 4, transaction);
                        transaction.commit();
                    }
                    if (inserted > 0) {
                        break;
                    }
                }
            }
            if (inserted != 4) {
                StringBuilder details = new StringBuilder();
                ResourceHandler<ItemResource> internal = Capabilities.Item.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(POS), null, null, null);
                details.append("internal: ").append(internal == null ? "null" : internal.size()).append("; tile: ")
                      .append(helper.getLevel().getBlockEntity(helper.absolutePos(POS))).append("; ");
                for (Direction side : Direction.values()) {
                    ResourceHandler<ItemResource> neo = Capabilities.Item.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(POS), null, null, side);
                    Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(POS), side);
                    details.append(side).append(": neo=").append(neo == null ? "null" : neo.getClass().getSimpleName() + "(" + neo.size() + ")")
                          .append(" fabric=").append(storage == null ? "null" : storage.getClass().getSimpleName()).append("; ");
                }
                throw helper.assertionException("Expected 4 raw iron to be inserted through Fabric's item storage, was %s. %s", inserted, details);
            }
            helper.succeed();
        });
    }
}
