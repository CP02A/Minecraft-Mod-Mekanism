package mekanism.tools.fabric;

import mekanism.fabric.shim.ShimModInitializer;
import mekanism.tools.client.ToolsClient;
import mekanism.shim.neoforged.bus.api.IEventBus;
import mekanism.shim.neoforged.fml.ModContainer;

/**
 * Constructs Mekanism: Tools' NeoForge style client mod class.
 */
public class ToolsShimClientMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new ToolsClient(container);
    }
}
