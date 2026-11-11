package mekanism.fabric.integration.computercraft;

import mekanism.common.integration.computercraft.CCPeripheralCapability;
import mekanism.common.integration.computercraft.MekanismComputerCraft;
import mekanism.fabric.shim.ShimModInitializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/// Constructs [MekanismComputerCraft], which NeoForge constructs as a `@Mod(depends = "computercraft")` class. Registered as Mekanism's
/// `mekanism:computercraft` entrypoint, which [mekanism.fabric.MekanismShimMod] only loads when CC: Tweaked is present.
public class MekanismComputerCraftShim implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new MekanismComputerCraft(modEventBus);
        //Common setup is fired once the capabilities of all mods (including Mekanism's modules) have been registered
        modEventBus.addListener(FMLCommonSetupEvent.class, _ -> CCPeripheralCapability.registerLookup());
    }
}
