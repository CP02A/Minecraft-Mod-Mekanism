package mekanism.generators.fabric;

import mekanism.fabric.shim.ShimModInitializer;
import mekanism.generators.client.GeneratorsClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Constructs Mekanism: Generators' NeoForge style client mod class.
 */
public class GeneratorsShimClientMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new GeneratorsClient(container);
    }
}
