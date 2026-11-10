package mekanism.fabric.shim;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Entrypoint ({@value ShimModLoader#ENTRYPOINT}) of a mod that is written against NeoForge's API, equivalent to the constructor of a NeoForge
 * {@code @Mod} class.
 */
@FunctionalInterface
public interface ShimModInitializer {

    void construct(ModContainer container, IEventBus modEventBus);
}
