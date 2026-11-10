package mekanism.tools.fabric;

import mekanism.fabric.shim.ShimModInitializer;
import mekanism.tools.common.MekanismTools;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Constructs Mekanism: Tools' NeoForge style mod class.
 */
public class ToolsShimMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new MekanismTools(container, modEventBus);
    }
}
