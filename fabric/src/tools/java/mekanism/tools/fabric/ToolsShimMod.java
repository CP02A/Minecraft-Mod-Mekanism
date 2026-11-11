package mekanism.tools.fabric;

import mekanism.fabric.shim.ShimModInitializer;
import mekanism.tools.common.MekanismTools;
import mekanism.tools.common.registries.ToolsRecipeSerializers;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import mekanism.shim.neoforged.bus.api.IEventBus;
import mekanism.shim.neoforged.fml.ModContainer;
import mekanism.shim.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Constructs Mekanism: Tools' NeoForge style mod class.
 */
public class ToolsShimMod implements ShimModInitializer {

    @Override
    public void construct(ModContainer container, IEventBus modEventBus) {
        new MekanismTools(container, modEventBus);
        //Sync the paxel recipes so that recipe viewers (JEI) can show them, see MekanismShimMod
        modEventBus.addListener(FMLCommonSetupEvent.class, event -> RecipeSynchronization.synchronizeRecipeSerializer(ToolsRecipeSerializers.PAXEL.get()));
    }
}
