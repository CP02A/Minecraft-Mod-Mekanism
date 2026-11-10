package mekanism.fabric.gametest;

import mekanism.generators.client.gui.GuiHeatGenerator;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.item.TrimmedArmorModel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Client game tests for Mekanism: Generators: renders some generators (screenshot in the run directory's screenshots folder), checks
 * the wind generator's composite model and Tools' trimmed armor item models, and opens a generator's GUI.
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
            context.runOnClient(client -> {
                //The wind generator uses NeoForge's composite model loader, with element rotations and a root transform
                BlockState windGenerator = BuiltInRegistries.BLOCK.getValue(Identifier.parse("mekanismgenerators:wind_generator")).defaultBlockState();
                Identifier particle = client.getModelManager().getBlockStateModelSet().get(windGenerator).particleMaterial().sprite().contents().name();
                if (particle.equals(MissingTextureAtlasSprite.getLocation())) {
                    throw new AssertionError("The wind generator's model is missing");
                }
                //Tools' armor uses NeoForge's trimmed armor item model
                ItemModel armorModel = client.getModelManager().getItemModel(Identifier.parse("mekanismtools:bronze_chestplate"));
                if (!(armorModel instanceof TrimmedArmorModel)) {
                    throw new AssertionError("Expected a trimmed armor item model for bronze chestplates, got " + armorModel);
                }
            });

            context.getInput().pressKey(options -> options.keyUse);
            context.waitFor(client -> client.gui.screen() instanceof GuiHeatGenerator);
            context.waitTicks(5);
            context.takeScreenshot("mekanism-heat-generator-gui");
            context.setScreen(() -> null);
            context.waitTicks(5);
        }
    }
}
