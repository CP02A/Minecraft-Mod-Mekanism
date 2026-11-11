package mekanism.additions.fabric;

import mekanism.additions.client.AdditionsClient;
import mekanism.fabric.shim.ShimModInitializer;
import mekanism.shim.neoforged.bus.api.IEventBus;
import mekanism.shim.neoforged.fml.ModContainer;

/**
 * Constructs Mekanism: Additions' NeoForge style client mod class.
 */
public class AdditionsShimClientMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new AdditionsClient(container);
    }
}
