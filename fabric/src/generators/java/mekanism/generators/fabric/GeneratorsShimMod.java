package mekanism.generators.fabric;

import mekanism.fabric.shim.ShimModInitializer;
import mekanism.generators.common.MekanismGenerators;
import mekanism.shim.neoforged.bus.api.IEventBus;
import mekanism.shim.neoforged.fml.ModContainer;

/**
 * Constructs Mekanism: Generators' NeoForge style mod class.
 */
public class GeneratorsShimMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new MekanismGenerators(container, modEventBus);
    }
}
