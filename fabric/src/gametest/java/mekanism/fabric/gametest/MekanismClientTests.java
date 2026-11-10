package mekanism.fabric.gametest;

import mekanism.client.MekanismClient;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.model.MekanismModelCache;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismFluids;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.Fluids;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Client game tests: loads a world with some of Mekanism's blocks and takes screenshots (in the run directory's screenshots folder) to check rendering, then
 * opens a machine's GUI.
 */
public class MekanismClientTests implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        context.takeScreenshot("mekanism-title-screen");
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            singleplayer.getServer().runCommand("time set day");
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~ ~ mekanism:basic_energy_cube");
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~ ~1 mekanism:osmium_ore");
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~ ~-1 mekanism:basic_fluid_tank");
            //Uses NeoForge's composite model loader
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~1 ~1 mekanism:basic_combining_factory[facing=west]");
            //In the crosshair
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~1 ~ mekanism:enrichment_chamber[facing=west]");
            singleplayer.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ -90 20");
            context.waitTicks(40);
            singleplayer.getConnection().waitForChunksRender();
            context.takeScreenshot("mekanism-blocks");

            //Model and texture events
            context.runOnClient(client -> {
                if (MekanismRenderer.energyIcon == null) {
                    throw new AssertionError("TextureAtlasStitchedEvent was not fired");
                }
                if (MekanismModelCache.INSTANCE.ENERGY_CORE.getBakedModel().isEmpty()) {
                    throw new AssertionError("ModelEvent.BakingCompleted was not fired or the standalone models are missing");
                }
                FluidStateModelSet fluidModels = client.getModelManager().getFluidStateModelSet();
                if (fluidModels.get(MekanismFluids.HYDROGEN.getFlowingFluid().value().defaultFluidState()) == fluidModels.get(Fluids.EMPTY.defaultFluidState())) {
                    throw new AssertionError("RegisterFluidModelsEvent was not fired");
                }
            });

            //Recipes are synced to the client via RecipesReceivedEvent
            int enriching = context.computeOnClient(client -> MekanismClient.clientRecipes().byType(MekanismRecipeType.ENRICHING.get()).size());
            if (enriching == 0) {
                throw new AssertionError("No enriching recipes were synced to the client");
            }

            context.getInput().pressKey(options -> options.keyUse);
            context.waitFor(client -> client.gui.screen() instanceof GuiMekanism<?>);
            context.waitTicks(5);
            context.takeScreenshot("mekanism-machine-gui");
            context.setScreen(() -> null);
            context.waitTicks(5);
        }
    }
}
