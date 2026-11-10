package mekanism.fabric.gametest;

import java.util.HashMap;
import java.util.Map;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyItem;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Tests energy interop with Team Reborn's energy API and the item capability bridge (NeoForge item capabilities <-> Fabric's item lookups).
 */
public class InteropTests implements ModInitializer {

    private static final BlockPos POS = new BlockPos(1, 1, 1);
    /**
     * Energy storages of the "Fabric mod" energy blocks (lodestones in the test environment), by position.
     */
    private static final Map<GlobalPos, SimpleEnergyStorage> FABRIC_ENERGY_BLOCKS = new HashMap<>();
    private static final long FABRIC_ITEM_CAPACITY = 10_000;

    @Override
    public void onInitialize() {
        //Stand-ins for blocks and items of other Fabric mods that only expose Team Reborn energy storages
        EnergyStorage.SIDED.registerForBlocks((level, pos, _, _, _) -> FABRIC_ENERGY_BLOCKS.computeIfAbsent(GlobalPos.of(level.dimension(), pos.immutable()),
              _ -> new SimpleEnergyStorage(100_000, 1_000, 1_000)), Blocks.LODESTONE);
        EnergyStorage.ITEM.registerForItems((_, context) -> SimpleEnergyItem.createStorage(context, FABRIC_ITEM_CAPACITY, 1_000, 1_000), Items.STICK);
    }

    private static ContainerItemContext fabricContext(SimpleContainer container) {
        return ContainerItemContext.ofSingleSlot(ContainerStorage.of(container, null).getSlot(0));
    }

    private static ItemAccess neoAccess(SimpleContainer container) {
        return ItemAccess.forHandlerIndex(VanillaContainerWrapper.of(container), 0);
    }

    @GameTest
    public void fabricCanChargeMekanismEnergyCube(GameTestHelper helper) {
        helper.setBlock(POS, MekanismBlocks.BASIC_ENERGY_CUBE.defaultState());
        helper.getLevel().getBlockEntity(helper.absolutePos(POS)).applyComponentsFromItemStack(new ItemStack(MekanismBlocks.BASIC_ENERGY_CUBE));
        //Mekanism's tiles finish initializing when they get loaded (on the next block entity tick)
        helper.runAfterDelay(2, () -> {
            long inserted = 0;
            for (Direction side : Direction.values()) {
                EnergyStorage storage = EnergyStorage.SIDED.find(helper.getLevel(), helper.absolutePos(POS), side);
                if (storage != null && storage.supportsInsertion()) {
                    try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                        inserted = storage.insert(500, transaction);
                        transaction.commit();
                    }
                    if (inserted > 0) {
                        break;
                    }
                }
            }
            helper.assertValueEqual(inserted, 500L, "energy inserted through Team Reborn's energy API");
            EnergyHandler handler = Capabilities.Energy.BLOCK.getCapability(helper.getLevel(), helper.absolutePos(POS), null, null, null);
            helper.assertTrue(handler != null, Component.literal("Expected a NeoForge energy handler for the energy cube"));
            helper.assertValueEqual(handler.getAmountAsLong(), 500L, "energy stored in the energy cube");
            helper.succeed();
        });
    }

    @GameTest
    public void mekanismCanChargeFabricEnergyBlock(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.LODESTONE);
        BlockPos absolute = helper.absolutePos(POS);
        FABRIC_ENERGY_BLOCKS.remove(GlobalPos.of(helper.getLevel().dimension(), absolute));
        EnergyHandler handler = Capabilities.Energy.BLOCK.getCapability(helper.getLevel(), absolute, null, null, Direction.UP);
        helper.assertTrue(handler != null, Component.literal("Expected a NeoForge energy handler for the Team Reborn energy block"));
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(5_000, transaction), 1_000, "energy inserted (limited by the max insert rate)");
            transaction.commit();
        }
        try (Transaction transaction = Transaction.openRoot()) {
            handler.insert(1_000, transaction);
            //Not committed
        }
        helper.assertValueEqual(FABRIC_ENERGY_BLOCKS.get(GlobalPos.of(helper.getLevel().dimension(), absolute)).getAmount(), 1_000L, "stored energy");
        helper.succeed();
    }

    @GameTest
    public void fabricCanChargeMekanismItem(GameTestHelper helper) {
        SimpleContainer container = new SimpleContainer(new ItemStack(MekanismItems.ENERGY_TABLET.get()));
        EnergyStorage storage = fabricContext(container).find(EnergyStorage.ITEM);
        helper.assertTrue(storage != null, Component.literal("The energy tablet should expose a Team Reborn energy storage"));
        long inserted;
        try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            inserted = storage.insert(1_000, transaction);
            transaction.commit();
        }
        helper.assertTrue(inserted > 0, Component.literal("The energy tablet should accept energy"));
        EnergyHandler handler = neoAccess(container).getCapability(Capabilities.Energy.ITEM);
        helper.assertTrue(handler != null, Component.literal("Expected a NeoForge energy handler for the energy tablet"));
        helper.assertValueEqual(handler.getAmountAsLong(), inserted, "energy stored in the tablet");
        helper.succeed();
    }

    @GameTest
    public void mekanismCanChargeFabricItem(GameTestHelper helper) {
        SimpleContainer container = new SimpleContainer(new ItemStack(Items.STICK));
        EnergyHandler handler = neoAccess(container).getCapability(Capabilities.Energy.ITEM);
        helper.assertTrue(handler != null, Component.literal("Expected a NeoForge energy handler for the Team Reborn energy item"));
        helper.assertValueEqual(handler.getCapacityAsLong(), FABRIC_ITEM_CAPACITY, "item energy capacity");
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(600, transaction), 600, "energy inserted");
            transaction.commit();
        }
        helper.assertValueEqual(SimpleEnergyItem.getStoredEnergyUnchecked(container.getItem(0)), 600L, "energy stored in the item");
        helper.succeed();
    }

    @GameTest
    public void fabricCanFillMekanismTankItem(GameTestHelper helper) {
        SimpleContainer container = new SimpleContainer(new ItemStack(MekanismBlocks.BASIC_FLUID_TANK));
        Storage<FluidVariant> storage = fabricContext(container).find(FluidStorage.ITEM);
        helper.assertTrue(storage != null, Component.literal("Mekanism's fluid tank item should expose a Fabric fluid storage"));
        try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            helper.assertValueEqual(storage.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, transaction), FluidConstants.BUCKET, "inserted droplets");
            transaction.commit();
        }
        ResourceHandler<FluidResource> handler = neoAccess(container).getCapability(Capabilities.Fluid.ITEM);
        helper.assertTrue(handler != null, Component.literal("Expected a NeoForge fluid handler for the fluid tank item"));
        helper.assertValueEqual(handler.getAmountAsLong(0), 1_000L, "stored millibuckets");
        helper.assertTrue(handler.getResource(0).getFluid() == Fluids.WATER, Component.literal("The tank item should contain water"));
        helper.succeed();
    }

    @GameTest
    public void mekanismCanReadFabricOnlyFluidItem(GameTestHelper helper) {
        //NeoForge doesn't provide a fluid capability for water bottles, but Fabric does
        SimpleContainer container = new SimpleContainer(PotionContents.createItemStack(Items.POTION, Potions.WATER));
        ResourceHandler<FluidResource> handler = neoAccess(container).getCapability(Capabilities.Fluid.ITEM);
        helper.assertTrue(handler != null, Component.literal("Expected a NeoForge fluid handler for the water bottle"));
        helper.assertTrue(handler.getResource(0).getFluid() == Fluids.WATER, Component.literal("The water bottle should contain water"));
        helper.assertValueEqual(handler.getAmountAsLong(0), FluidConstants.BOTTLE / 81, "millibuckets in the water bottle");
        helper.succeed();
    }
}
