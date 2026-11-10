package mekanism.fabric;

import mekanism.common.Mekanism;
import mekanism.fabric.shim.ShimModInitializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Constructs Mekanism's NeoForge style mod class.
 */
public class MekanismShimMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new Mekanism(container, modEventBus);
    }
}
