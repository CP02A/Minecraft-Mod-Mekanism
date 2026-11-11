package mekanism.fabric;

import mekanism.common.Mekanism;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.registries.MekanismRecipeSerializersInternal;
import mekanism.fabric.shim.ShimModInitializer;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Constructs Mekanism's NeoForge style mod class.
 */
public class MekanismShimMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new Mekanism(container, modEventBus);
        constructIntegration(MekanismHooks.CC_MOD_ID, container, modEventBus);
        modEventBus.addListener(FMLCommonSetupEvent.class, event -> {
            //Recipe viewers (JEI) only see the crafting recipes that are synced to the client. On NeoForge all recipes of the requested types are synced,
            // on Fabric only those whose serializer is registered for synchronization (JEI only registers the minecraft ones)
            RecipeSynchronization.synchronizeRecipeSerializer(MekanismRecipeSerializersInternal.MEK_DATA.get());
            RecipeSynchronization.synchronizeRecipeSerializer(MekanismRecipeSerializersInternal.CLEAR_CONFIGURATION.get());
            RecipeSynchronization.synchronizeRecipeSerializer(MekanismRecipeSerializersInternal.BIN_INSERT.get());
            RecipeSynchronization.synchronizeRecipeSerializer(MekanismRecipeSerializersInternal.BIN_EXTRACT.get());
        });
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
