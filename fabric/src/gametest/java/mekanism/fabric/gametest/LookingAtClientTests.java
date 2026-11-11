package mekanism.fabric.gametest;

import java.util.List;
import java.util.Optional;
import mcjty.theoneprobe.TheOneProbe;
import mcp.mobius.waila.access.DataReader;
import mcp.mobius.waila.api.IWailaConfig;
import mcp.mobius.waila.gui.hud.TooltipPosition;
import mcp.mobius.waila.registry.Registrar;
import mekanism.api.SerializationConstants;
import mekanism.common.integration.lookingat.EnergyElement;
import mekanism.common.integration.lookingat.ILookingAtElement;
import mekanism.common.integration.lookingat.LookingAtElementType;
import mekanism.common.integration.lookingat.LookingAtUtils;
import mekanism.common.integration.lookingat.jade.JadeBuiltinRemover;
import mekanism.common.integration.lookingat.jade.JadeTooltipRenderer;
import mekanism.common.integration.lookingat.theoneprobe.TOPChemicalElement;
import mekanism.common.integration.lookingat.theoneprobe.TOPEnergyElement;
import mekanism.common.integration.lookingat.theoneprobe.TOPFluidElement;
import mekanism.common.integration.lookingat.wthit.WTHITLookingAtHelper;
import mekanism.common.integration.lookingat.wthit.WTHITTooltipRenderer;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityEnergyCube;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import snownee.jade.Jade;
import snownee.jade.JadeClient;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.JadeIds;
import snownee.jade.impl.Tooltip;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.overlay.WailaTickHandler;

/**
 * Client game test of the block info overlay integrations: their client plugins registered Mekanism's tooltip renderers, configs and element
 * factories, and looking at an energy cube shows Mekanism's energy info, synced from the server, in Jade's and WTHIT's overlays (screenshots).
 */
public class LookingAtClientTests implements FabricClientGameTest {

    private static final long ENERGY = 1_234_567;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(_ -> checkRegistrations());
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            //Only show one overlay at a time (The One Probe needs its probe to show tanks, its basic overlay may show as well)
            context.runOnClient(_ -> setWthitEnabled(false));
            BlockPos cube = singleplayer.getServer().computeOnServer(server -> {
                ServerLevel level = server.overworld();
                BlockPos pos = server.getPlayerList().getPlayers().getFirst().blockPosition().offset(0, 1, -3);
                level.setBlockAndUpdate(pos, MekanismBlocks.BASIC_ENERGY_CUBE.defaultState());
                ((TileEntityEnergyCube) level.getBlockEntity(pos)).energyContainer().setEnergy(ENERGY, null);
                return pos;
            });
            context.waitTicks(5);
            //Look at the top edge of the cube's frame (its centre has no shape)
            context.runOnClient(client -> client.player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(cube.getX() + 0.5, cube.getY() + 0.9, cube.getZ() + 1)));
            try {
                waitFor(context, "Jade to show Mekanism's energy of the energy cube", () -> context.computeOnClient(_ -> jadeShowsEnergy(cube)));
            } catch (AssertionError e) {
                context.takeScreenshot("mekanism-jade-energy-cube-failed");
                throw new AssertionError(e.getMessage() + ", Jade state: " + context.computeOnClient(_ -> jadeDebug()), e);
            }
            context.waitTicks(5);
            context.takeScreenshot("mekanism-jade-energy-cube");

            context.runOnClient(_ -> {
                Jade.config().general().setDisplayTooltip(false);
                setWthitEnabled(true);
            });
            waitFor(context, "WTHIT to receive Mekanism's energy of the energy cube", () -> context.computeOnClient(_ -> wthitHasEnergy()));
            context.waitTicks(5);
            context.takeScreenshot("mekanism-wthit-energy-cube");
            context.runOnClient(_ -> Jade.config().general().setDisplayTooltip(true));
        }
    }

    private static void waitFor(ClientGameTestContext context, String what, java.util.function.BooleanSupplier condition) {
        for (int tick = 0; !condition.getAsBoolean(); tick++) {
            if (tick > 200) {
                throw new AssertionError("Timed out waiting for " + what);
            }
            context.waitTick();
        }
    }

    private static void checkRegistrations() {
        Block cube = MekanismBlocks.BASIC_ENERGY_CUBE.defaultState().getBlock();
        //Jade
        WailaClientRegistration jade = WailaClientRegistration.instance();
        List<?> jadeProviders = jade.getBlockProviders(cube, _ -> true);
        check(jadeProviders.stream().anyMatch(JadeTooltipRenderer.class::isInstance), "Jade block component providers contain Mekanism's tooltip renderer: " + jadeProviders);
        check(jadeProviders.stream().anyMatch(JadeBuiltinRemover.class::isInstance), "Jade block component providers contain Mekanism's builtin remover: " + jadeProviders);
        for (var id : List.of(LookingAtUtils.ENERGY, LookingAtUtils.FLUID, LookingAtUtils.CHEMICAL)) {
            check(jade.hasConfig(id), "Jade has the config " + id);
        }
        check(!jade.rayTraceCallback.callbacks().isEmpty(), "Jade has ray trace callbacks (Mekanism's bounding block redirection)");
        //WTHIT
        Registrar wthit = Registrar.get();
        check(wthit.blockComponent.get(TooltipPosition.BODY).get(cube).stream().anyMatch(entry -> entry.instance().instance() instanceof WTHITTooltipRenderer),
              "WTHIT block body providers contain Mekanism's tooltip renderer");
        check(!wthit.blockOverride.get(MekanismBlocks.BOUNDING_BLOCK.get()).isEmpty(), "WTHIT has Mekanism's bounding block override");
        //The One Probe (the client creates the elements from the server's probe info with the registered factories)
        check(TheOneProbe.theOneProbeImp.getElementFactory(LookingAtUtils.ENERGY) instanceof TOPEnergyElement.Factory, "The One Probe has Mekanism's energy element factory");
        check(TheOneProbe.theOneProbeImp.getElementFactory(LookingAtUtils.FLUID) instanceof TOPFluidElement.Factory, "The One Probe has Mekanism's fluid element factory");
        check(TheOneProbe.theOneProbeImp.getElementFactory(LookingAtUtils.CHEMICAL) instanceof TOPChemicalElement.ChemicalElementFactory,
              "The One Probe has Mekanism's chemical element factory");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Expected: " + message);
        }
    }

    private static void setWthitEnabled(boolean enabled) {
        ((mcp.mobius.waila.config.WailaConfig) IWailaConfig.get()).getGeneral().setDisplayTooltip(enabled);
    }

    /**
     * Jade's current tooltip is for the energy cube, contains Mekanism's energy element instead of Jade's builtin energy storage, and the server
     * data it was built from has the cube's energy.
     */
    private static boolean jadeShowsEnergy(BlockPos cube) {
        WailaTickHandler tickHandler = JadeClient.tickHandler();
        if (tickHandler.state == null || tickHandler.rootElement == null || !(tickHandler.state.accessor() instanceof BlockAccessor accessor)
            || !accessor.getPosition().equals(cube)) {
            return false;
        }
        Tooltip tooltip = tickHandler.rootElement.getTooltip();
        if (tooltip.get(LookingAtUtils.ENERGY).isEmpty()) {
            return false;
        }
        check(tooltip.get(JadeIds.UNIVERSAL_ENERGY_STORAGE).isEmpty(), "Jade's builtin energy storage line is removed");
        EnergyElement energy = energy(decode(accessor));
        return energy != null && energy.getEnergy() == ENERGY;
    }

    private static String jadeDebug() {
        WailaTickHandler tickHandler = JadeClient.tickHandler();
        if (tickHandler.state == null) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            return "no state, hit " + mc.hitResult + ", display " + Jade.config().general().shouldDisplayTooltip() + ", pinned "
                   + snownee.jade.api.ui.JadeUI.isPinned() + ", hide with gui " + snownee.jade.util.ClientProxy.shouldHideWithGui(mc, mc.gui.screen()) + ", screen "
                   + mc.gui.screen() + ", target " + snownee.jade.overlay.RayTracing.INSTANCE.getTarget();
        }
        Accessor<?> accessor = tickHandler.state.accessor();
        return accessor + " at " + (accessor instanceof BlockAccessor block ? block.getPosition() + " " + block.getBlockState() : "?") + ", server data "
               + accessor.getServerData() + ", tooltip " + (tickHandler.rootElement == null ? null : tickHandler.rootElement.getTooltip().getNarration());
    }

    @Nullable
    private static List<ILookingAtElement> decode(Accessor<?> accessor) {
        Tag tag = accessor.getServerData().get(SerializationConstants.MEK_DATA);
        if (tag == null) {
            return null;
        }
        Optional<List<ILookingAtElement>> elements = accessor.decodeFromNbt(LookingAtElementType.ELEMENT_LIST_STREAM_CODEC, tag);
        return elements.orElse(null);
    }

    @Nullable
    private static EnergyElement energy(@Nullable List<ILookingAtElement> elements) {
        if (elements == null) {
            return null;
        }
        return elements.stream().filter(EnergyElement.class::isInstance).map(EnergyElement.class::cast).findFirst().orElse(null);
    }

    private static boolean wthitHasEnergy() {
        WTHITLookingAtHelper data = DataReader.CLIENT.get(WTHITLookingAtHelper.TYPE);
        EnergyElement energy = data == null ? null : energy(data.elements);
        return energy != null && energy.getEnergy() == ENERGY;
    }
}
