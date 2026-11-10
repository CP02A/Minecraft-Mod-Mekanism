package mekanism.fabric;

import mekanism.client.MekanismClient;
import mekanism.fabric.shim.ShimModInitializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Constructs Mekanism's NeoForge style client mod class.
 */
public class MekanismShimClientMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new MekanismClient(container, modEventBus);
    }
}
