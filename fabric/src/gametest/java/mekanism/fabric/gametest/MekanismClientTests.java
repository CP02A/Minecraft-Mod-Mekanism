package mekanism.fabric.gametest;

import mekanism.client.MekanismClient;
import mekanism.client.gui.GuiMekanism;
import com.mojang.blaze3d.platform.InputConstants;
import io.netty.buffer.Unpooled;
import java.util.List;
import mekanism.api.RelativeSide;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.button.SideDataButton;
import mekanism.client.gui.element.tab.window.GuiRecipeBookWindowTab;
import mekanism.client.gui.element.tab.window.GuiSideConfigurationTab;
import mekanism.client.gui.element.window.GuiRecipeBookWindow;
import mekanism.client.gui.element.window.GuiSideConfiguration;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.model.MekanismModelCache;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.interfaces.ISideConfiguration;
import mekanism.common.util.WorldUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.level.Level;
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
        checkModdedRecipeBookSettings();
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            singleplayer.getServer().runCommand("time set day");
            singleplayer.getServer().runCommand("recipe give @a *");
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~ ~ mekanism:basic_energy_cube");
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~ ~1 mekanism:osmium_ore");
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~ ~-1 mekanism:basic_fluid_tank");
            //Uses NeoForge's composite model loader
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~1 ~1 mekanism:basic_combining_factory[facing=west]");
            //Connected transmitters use model data
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~2 ~-1 mekanism:basic_universal_cable");
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~2 ~-2 mekanism:basic_universal_cable");
            //In the crosshair
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~1 ~ mekanism:enrichment_chamber[facing=west]");
            singleplayer.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ -90 20");
            context.waitTicks(40);
            singleplayer.getConnection().waitForChunksRender();
            context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
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

            //Open the side configuration window, and change the bottom side's configuration (sent to the server)
            clickElement(context, context.computeOnClient(client -> findElement(client.gui.screen().children(), GuiSideConfigurationTab.class)));
            context.waitFor(client -> client.gui.screen() instanceof GuiMekanism<?> gui && gui.getWindows().stream().anyMatch(GuiSideConfiguration.class::isInstance));
            context.waitTicks(5);
            BlockPos machinePos = context.computeOnClient(client -> ((GuiMekanismTile<?, ?>) client.gui.screen()).getTileEntity().getBlockPos());
            DataType before = context.computeOnClient(client -> bottomDataType(client.player.level(), machinePos));
            if (before != singleplayer.getServer().computeOnServer(server -> bottomDataType(server.overworld(), machinePos))) {
                throw new AssertionError("Side configuration is not synced to the client");
            }
            clickElement(context, context.computeOnClient(client -> {
                GuiWindow window = ((GuiMekanism<?>) client.gui.screen()).getWindows().stream().filter(GuiSideConfiguration.class::isInstance).findFirst().orElseThrow();
                //The first side data button is the bottom side
                return findElement(window.children(), SideDataButton.class);
            }));
            for (int tick = 0; singleplayer.getServer().computeOnServer(server -> bottomDataType(server.overworld(), machinePos)) == before; tick++) {
                if (tick == 100) {
                    throw new AssertionError("Side configuration change was not applied on the server");
                }
                context.waitTick();
            }
            context.waitFor(client -> bottomDataType(client.player.level(), machinePos) != before);
            context.waitTicks(5);
            context.takeScreenshot("mekanism-side-config");

            //Open Mekanism's recipe book (modded recipe book type and search category)
            context.runOnClient(client -> List.copyOf(((GuiMekanism<?>) client.gui.screen()).getWindows()).forEach(GuiWindow::close));
            context.waitTicks(2);
            clickElement(context, context.computeOnClient(client -> findElement(client.gui.screen().children(), GuiRecipeBookWindowTab.class)));
            context.waitFor(client -> client.gui.screen() instanceof GuiMekanism<?> gui && gui.getWindows().stream().anyMatch(GuiRecipeBookWindow.class::isInstance));
            context.waitTicks(5);
            context.takeScreenshot("mekanism-recipe-book");
            context.setScreen(() -> null);
            context.waitTicks(5);
        }
    }

    /**
     * Checks that the settings of modded recipe book types are saved and synced.
     */
    private static void checkModdedRecipeBookSettings() {
        RecipeBookType type = RecipeBookType.valueOf("MEKANISM_ENRICHING");
        RecipeBookSettings settings = new RecipeBookSettings();
        settings.setOpen(type, true);
        settings.setFiltering(type, true);
        Tag saved = RecipeBookSettings.MAP_CODEC.codec().encodeStart(NbtOps.INSTANCE, settings).getOrThrow();
        RecipeBookSettings loaded = RecipeBookSettings.MAP_CODEC.codec().parse(NbtOps.INSTANCE, saved).getOrThrow();
        if (!loaded.isOpen(type) || !loaded.isFiltering(type)) {
            throw new AssertionError("Modded recipe book settings were not saved: " + saved);
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        RecipeBookSettings.STREAM_CODEC.encode(buf, settings);
        RecipeBookSettings synced = RecipeBookSettings.STREAM_CODEC.decode(buf);
        if (!synced.isOpen(type) || !synced.isFiltering(type) || buf.isReadable()) {
            throw new AssertionError("Modded recipe book settings were not synced");
        }
    }

    private static DataType bottomDataType(Level level, BlockPos pos) {
        TileEntityMekanism tile = WorldUtils.getTileEntity(TileEntityMekanism.class, level, pos);
        if (!(tile instanceof ISideConfiguration config)) {
            throw new AssertionError("No configurable machine at " + pos);
        }
        TileComponentConfig component = config.getConfig();
        return component.getConfig(component.getTransmissions().getFirst()).getDataType(RelativeSide.BOTTOM);
    }

    private static <T extends GuiEventListener> T findElement(List<? extends GuiEventListener> elements, Class<T> type) {
        return elements.stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow(() -> new AssertionError("No " + type.getSimpleName()));
    }

    /**
     * Clicks the center of the given widget.
     */
    private static void clickElement(ClientGameTestContext context, AbstractWidget widget) {
        double scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
        context.getInput().setCursorPos((widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }
}
