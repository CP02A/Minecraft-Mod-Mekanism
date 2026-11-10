package mekanism.additions.fabric;

import mekanism.additions.client.AdditionsClient;
import mekanism.fabric.shim.ShimModInitializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Constructs Mekanism: Additions' NeoForge style client mod class.
 */
public class AdditionsShimClientMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new AdditionsClient(container);
    }
}
