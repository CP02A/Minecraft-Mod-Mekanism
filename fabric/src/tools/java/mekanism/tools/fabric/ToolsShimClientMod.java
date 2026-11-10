package mekanism.tools.fabric;

import mekanism.fabric.shim.ShimModInitializer;
import mekanism.tools.client.ToolsClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Constructs Mekanism: Tools' NeoForge style client mod class.
 */
public class ToolsShimClientMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new ToolsClient(container);
    }
}
