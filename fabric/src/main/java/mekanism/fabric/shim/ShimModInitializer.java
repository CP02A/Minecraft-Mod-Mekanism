package mekanism.fabric.shim;

import mekanism.shim.neoforged.bus.api.IEventBus;
import mekanism.shim.neoforged.fml.ModContainer;

/**
 * Entrypoint ({@value ShimModLoader#ENTRYPOINT}) of a mod that is written against NeoForge's API, equivalent to the constructor of a NeoForge
 * {@code @Mod} class.
 */
@FunctionalInterface
public interface ShimModInitializer {

    void construct(ModContainer container, IEventBus modEventBus);
}
