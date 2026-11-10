package mekanism.fabric.gametest;

import mekanism.generators.client.gui.GuiHeatGenerator;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Client game tests for Mekanism: Generators: renders some generators (screenshot in the run directory's screenshots folder) and opens a generator's GUI.
 */
public class GeneratorsClientTests implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            singleplayer.getServer().runCommand("time set day");
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~3 ~ ~2 mekanismgenerators:wind_generator[facing=west]");
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~ ~-1 mekanismgenerators:solar_generator[facing=west]");
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~ ~1 mekanismgenerators:gas_burning_generator[facing=west]");
            //In the crosshair
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~1 ~ mekanismgenerators:heat_generator[facing=west]");
            singleplayer.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ -90 20");
            context.waitTicks(40);
            singleplayer.getConnection().waitForChunksRender();
            context.takeScreenshot("mekanism-generators");

            context.getInput().pressKey(options -> options.keyUse);
            context.waitFor(client -> client.gui.screen() instanceof GuiHeatGenerator);
            context.waitTicks(5);
            context.takeScreenshot("mekanism-heat-generator-gui");
            context.setScreen(() -> null);
            context.waitTicks(5);
        }
    }
}
