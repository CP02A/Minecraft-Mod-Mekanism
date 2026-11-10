package mekanism.fabric.gametest;

import mekanism.client.MekanismClient;
import mekanism.client.gui.GuiMekanism;
import mekanism.common.recipe.MekanismRecipeType;
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
            //In the crosshair
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~1 ~ mekanism:enrichment_chamber[facing=west]");
            singleplayer.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ -90 20");
            context.waitTicks(40);
            singleplayer.getConnection().waitForChunksRender();
            context.takeScreenshot("mekanism-blocks");

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
