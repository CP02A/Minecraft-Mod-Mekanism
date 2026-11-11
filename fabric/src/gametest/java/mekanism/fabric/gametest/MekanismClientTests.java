package mekanism.fabric.gametest;

import mekanism.client.MekanismClient;
import mekanism.client.key.MekanismKeyHandler;
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
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import mekanism.common.registries.MekanismDataComponents;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
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
 * opens a machine's GUI. Also renders fluid container items and connected transmitters.
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

            checkFluidContainerItems(context, singleplayer);
            checkItemTooltips(context, singleplayer);
            checkTransmitterConnections(context, singleplayer);
        }
    }

    /**
     * Renders filled fluid tanks (Mekanism's special model renderer) and buckets (NeoForge's {@code neoforge:fluid_container} item model) in the hotbar
     * and in hand.
     */
    private static void checkFluidContainerItems(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            fillSlot(player, 0, new ItemStack(MekanismBlocks.BASIC_FLUID_TANK), FluidResource.of(Fluids.WATER));
            fillSlot(player, 1, new ItemStack(MekanismBlocks.ADVANCED_FLUID_TANK), MekanismFluids.BRINE.asResource());
            player.getInventory().setItem(2, new ItemStack(MekanismFluids.BRINE.getBucket().asItem()));
            player.getInventory().setItem(3, new ItemStack(MekanismFluids.HEAVY_WATER.getBucket().asItem()));
            player.getInventory().setItem(4, new ItemStack(MekanismFluids.HYDROGEN.getBucket().asItem()));
            player.getInventory().setItem(5, new ItemStack(Items.LAVA_BUCKET));
        });
        context.runOnClient(client -> {
            ItemModel model = client.getModelManager().getItemModel(BuiltInRegistries.ITEM.getKey(MekanismFluids.BRINE.getBucket().asItem()));
            if (!(model instanceof DynamicFluidContainerModel)) {
                throw new AssertionError("Expected a dynamic fluid container model for the brine bucket, got " + model);
            }
            //The portable QIO dashboard's LED layer is emissive (item layer neoforge_data)
            ItemModel dashboard = client.getModelManager().getItemModel(net.minecraft.resources.Identifier.parse("mekanism:portable_qio_dashboard"));
            try {
                java.lang.reflect.Field field = dashboard.getClass().getDeclaredField("itemQuads");
                field.setAccessible(true);
                net.minecraft.client.resources.model.geometry.ItemQuads quads = (net.minecraft.client.resources.model.geometry.ItemQuads) field.get(dashboard);
                if (quads.all().stream().noneMatch(quad -> quad.materialInfo().tintIndex() == 1 && quad.materialInfo().lightEmission() == 15)) {
                    throw new AssertionError("The portable QIO dashboard's second layer should have a light emission of 15");
                }
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Unexpected portable QIO dashboard item model " + dashboard, e);
            }
        });
        context.waitFor(client -> !client.player.getInventory().getItem(5).isEmpty());
        context.getInput().pressKey(options -> options.keyHotbarSlots[2]);
        context.waitTicks(10);
        context.takeScreenshot("mekanism-held-bucket");
        context.getInput().pressKey(options -> options.keyHotbarSlots[1]);
        context.waitTicks(10);
        context.takeScreenshot("mekanism-held-fluid-tank");
    }

    /**
     * Checks that NeoForge's tooltip appenders (RegisterTooltipAppendersEvent) add Mekanism's data component tooltips, and screenshots a hovered tooltip in
     * the inventory. Expects a filled basic fluid tank in hotbar slot 0 ({@link #checkFluidContainerItems}).
     */
    private static void checkItemTooltips(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runOnServer(server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().setItem(6, new ItemStack(MekanismBlocks.BASIC_ENERGY_CUBE));
            EnergyHandler handler = ItemAccess.forPlayerSlot(player, 6).getCapability(Capabilities.Energy.ITEM);
            if (handler == null) {
                throw new AssertionError("No energy handler for the basic energy cube");
            }
            try (Transaction transaction = Transaction.openRoot()) {
                handler.insert(1_000_000, transaction);
                transaction.commit();
            }
        });
        context.waitFor(client -> !client.player.getInventory().getItem(6).isEmpty() && client.player.getInventory().getItem(6).has(MekanismDataComponents.ATTACHED_ENERGY));
        context.runOnClient(client -> {
            List<String> energyCube = tooltipLines(client, client.player.getInventory().getItem(6));
            if (energyCube.stream().noneMatch(line -> line.startsWith("Stored energy: ") && !line.endsWith(" 0 FE") && !line.endsWith(" 0 J"))) {
                throw new AssertionError("Charged energy cube tooltip is missing its stored energy: " + energyCube);
            }
            List<String> fluidTank = tooltipLines(client, client.player.getInventory().getItem(0));
            if (fluidTank.stream().skip(1).noneMatch(line -> line.contains("Water") && line.contains("mB"))) {
                throw new AssertionError("Filled fluid tank tooltip is missing its stored fluid: " + fluidTank);
            }
            if (fluidTank.stream().noneMatch(line -> line.startsWith("Capacity"))) {
                throw new AssertionError("Filled fluid tank tooltip is missing its capacity: " + fluidTank);
            }
        });
        //Hover the charged energy cube in the survival inventory
        String gameMode = context.computeOnClient(client -> client.gameMode.getPlayerMode().getSerializedName());
        singleplayer.getServer().runCommand("gamemode survival @a");
        context.waitFor(client -> !client.player.hasInfiniteMaterials());
        context.setScreen(() -> new InventoryScreen(context.computeOnClient(client -> client.player)));
        context.waitTicks(5);
        double[] cursor = context.computeOnClient(client -> {
            InventoryScreen screen = (InventoryScreen) client.gui.screen();
            //Hotbar slot 6 of the inventory menu: the gui is 176x166, hotbar slots start at (8, 142) and are 18 pixels apart
            int x = (screen.width - 176) / 2 + 8 + 6 * 18 + 8;
            int y = (screen.height - 166) / 2 + 142 + 8;
            double scale = client.getWindow().getGuiScale();
            return new double[]{x * scale, y * scale};
        });
        context.getInput().setCursorPos(cursor[0], cursor[1]);
        context.runOnClient(client -> client.gui.toastManager().clear());
        //Show the details (the stored energy)
        context.getInput().holdKey(MekanismKeyHandler.detailsKey);
        context.waitTicks(5);
        context.takeScreenshot("mekanism-energy-cube-tooltip");
        context.getInput().releaseKey(MekanismKeyHandler.detailsKey);
        context.setScreen(() -> null);
        singleplayer.getServer().runCommand("gamemode " + gameMode + " @a");
        context.waitTicks(5);
    }

    private static List<String> tooltipLines(net.minecraft.client.Minecraft client, ItemStack stack) {
        return stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, ALL_INFORMATION).stream().map(Component::getString).toList();
    }

    /**
     * Like a recipe viewer's flag: shows Mekanism's details without holding the details key.
     */
    private static final TooltipFlag ALL_INFORMATION = new TooltipFlag() {
        @Override
        public boolean isAdvanced() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }

        @Override
        public boolean shouldDisplayAllInformation() {
            return true;
        }
    };

    private static void fillSlot(ServerPlayer player, int slot, ItemStack stack, FluidResource fluid) {
        player.getInventory().setItem(slot, stack);
        ResourceHandler<FluidResource> handler = ItemAccess.forPlayerSlot(player, slot).getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) {
            throw new AssertionError("No fluid handler for " + stack);
        }
        //The tank's insertion rate is limited, so fill it in multiple operations
        for (int i = 0; i < 100; i++) {
            handler = ItemAccess.forPlayerSlot(player, slot).getCapability(Capabilities.Fluid.ITEM);
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = handler.insert(fluid, Integer.MAX_VALUE, transaction);
                transaction.commit();
                if (inserted == 0) {
                    break;
                }
            }
        }
        if (handler.getAmountAsLong(0) < handler.getCapacityAsLong(0, fluid)) {
            throw new AssertionError("Could not fill " + stack + ": " + handler.getAmountAsLong(0) + " of " + handler.getCapacityAsLong(0, fluid));
        }
    }

    /**
     * Connects two energy cubes with universal cables and checks that the connections (to the cables and the cubes) reach the client (rendered through model data).
     */
    private static void checkTransmitterConnections(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        BlockPos start = singleplayer.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition()).offset(10, 0, 0);
        singleplayer.getServer().runCommand("setblock " + pos(start) + " mekanism:basic_energy_cube");
        singleplayer.getServer().runCommand("setblock " + pos(start.south()) + " mekanism:basic_universal_cable");
        singleplayer.getServer().runCommand("setblock " + pos(start.south(2)) + " mekanism:basic_universal_cable");
        singleplayer.getServer().runCommand("setblock " + pos(start.south(3)) + " mekanism:basic_energy_cube");
        singleplayer.getServer().runCommand("setblock " + pos(start.south().above()) + " mekanism:basic_universal_cable");
        //Apply the energy cube item's default side configuration like placing it would, setblock leaves all sides disabled (also on NeoForge)
        singleplayer.getServer().runOnServer(server -> {
            for (BlockPos cube : List.of(start, start.south(3))) {
                server.overworld().getBlockEntity(cube).applyComponentsFromItemStack(new ItemStack(MekanismBlocks.BASIC_ENERGY_CUBE));
            }
        });
        singleplayer.getServer().runCommand("tp @p " + (start.getX() - 2) + " " + start.getY() + " " + (start.getZ() + 2) + " -90 35");
        context.waitTicks(40);
        singleplayer.getConnection().waitForChunksRender();
        context.takeScreenshot("mekanism-transmitter-connections");
        assertConnections(context, singleplayer, start.south(), Direction.NORTH, Direction.SOUTH, Direction.UP);
        assertConnections(context, singleplayer, start.south(2), Direction.NORTH, Direction.SOUTH);
        //The model data the chunk meshing uses comes from the client level's ModelDataManager (snapshotted per meshing region)
        context.runOnClient(client -> {
            if (client.level.getModelDataManager().getAt(start.south()) == net.neoforged.neoforge.model.data.ModelData.EMPTY) {
                throw new AssertionError("The client's ModelDataManager has no model data for the transmitter at " + start.south());
            }
            var region = new net.minecraft.client.renderer.chunk.RenderRegionCache().createRegion(client.level,
                  net.minecraft.core.SectionPos.asLong(start.south()));
            if (region.getModelData(start.south()) != client.level.getModelDataManager().getAt(start.south())) {
                throw new AssertionError("The chunk meshing region's model data snapshot doesn't match the client's ModelDataManager");
            }
        });
    }

    /**
     * Checks that the transmitter at the given position is connected to the given sides, and that the client knows about the same connections as the
     * server (including connections to the energy cubes, which depend on their side configuration).
     */
    private static void assertConnections(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos pos, Direction... sides) {
        byte serverConnections = singleplayer.getServer().computeOnServer(server -> connections(server.overworld(), pos));
        byte clientConnections = context.computeOnClient(client -> connections(client.player.level(), pos));
        if (serverConnections != clientConnections) {
            throw new AssertionError("Transmitter connections at " + pos + " differ between the server (" + Integer.toBinaryString(serverConnections) +
                                     ") and the client (" + Integer.toBinaryString(clientConnections) + ")");
        }
        for (Direction side : sides) {
            if (!Transmitter.connectionMapContainsSide(clientConnections, side)) {
                throw new AssertionError("Transmitter at " + pos + " is not connected to the " + side + " (connections: " + Integer.toBinaryString(clientConnections) + ")");
            }
        }
    }

    private static byte connections(Level level, BlockPos pos) {
        if (!(WorldUtils.getTileEntity(level, pos) instanceof TileEntityTransmitter transmitter)) {
            throw new AssertionError("No transmitter at " + pos);
        }
        return transmitter.getTransmitter().getAllCurrentConnections();
    }

    private static String pos(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
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
