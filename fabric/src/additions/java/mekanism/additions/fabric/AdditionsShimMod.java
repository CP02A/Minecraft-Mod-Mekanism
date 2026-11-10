package mekanism.additions.fabric;

import mekanism.additions.common.MekanismAdditions;
import mekanism.fabric.shim.ShimModInitializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Constructs Mekanism: Additions' NeoForge style mod class.
 */
public class AdditionsShimMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new MekanismAdditions(container, modEventBus);
    }
}
