package mekanism.fabric;

import mekanism.common.Mekanism;
import mekanism.common.integration.MekanismHooks;
import mekanism.fabric.shim.ShimModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Constructs Mekanism's NeoForge style mod class.
 */
public class MekanismShimMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new Mekanism(container, modEventBus);
        constructIntegration(MekanismHooks.CC_MOD_ID, container, modEventBus);
    }

    /**
     * Constructs the integration classes that NeoForge constructs as {@code @Mod(value = "mekanism", depends = modId)} classes: the {@code mekanism:<modId>}
     * entrypoints, which are only loaded if the mod is present.
     */
    private static void constructIntegration(String modId, ModContainer container, IEventBus modEventBus) {
        if (FabricLoader.getInstance().isModLoaded(modId)) {
            for (ShimModInitializer integration : FabricLoader.getInstance().getEntrypoints(Mekanism.MODID + ":" + modId, ShimModInitializer.class)) {
                integration.construct(container, modEventBus);
            }
        }
    }
}
