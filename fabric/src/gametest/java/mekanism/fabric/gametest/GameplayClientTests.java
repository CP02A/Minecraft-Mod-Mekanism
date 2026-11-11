package mekanism.fabric.gametest;

import java.util.UUID;
import mekanism.api.security.SecurityMode;
import mekanism.client.gui.item.GuiPortableTeleporter;
import mekanism.common.component.FrequencyAware;
import mekanism.common.lib.frequency.Frequency.FrequencyIdentity;
import mekanism.common.registries.MekanismDataComponents;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Client game test of an item-held GUI and its packets: opens the portable teleporter's GUI (menu opening for a held item, with the item's hand as
 * extra data), creates a frequency from the GUI (client to server packet) and checks that the server stored it on the item and synced it back.
 */
public class GameplayClientTests implements FabricClientGameTest {

    private static final String FREQUENCY = "fabric_client_test";

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            singleplayer.getServer().runCommand("item replace entity @p weapon.mainhand with mekanism:portable_teleporter");
            //Look up, so that using the item doesn't target a block
            singleplayer.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 -90");
            context.waitTicks(5);
            //The first use claims the (unowned) item, the next one opens the GUI
            for (int attempt = 0; attempt < 5 && !context.computeOnClient(client -> client.gui.screen() instanceof GuiPortableTeleporter); attempt++) {
                context.getInput().pressKey(options -> options.keyUse);
                context.waitTicks(10);
            }
            context.waitFor(client -> client.gui.screen() instanceof GuiPortableTeleporter);
            context.takeScreenshot("mekanism-portable-teleporter-gui");
            context.runOnClient(client -> {
                UUID player = client.player.getUUID();
                ((GuiPortableTeleporter) client.gui.screen()).sendSetFrequency(new FrequencyIdentity(FREQUENCY, SecurityMode.PUBLIC, player));
            });
            for (int tick = 0; !FREQUENCY.equals(singleplayer.getServer().computeOnServer(server ->
                  frequencyName(server.getPlayerList().getPlayers().getFirst().getMainHandItem()))); tick++) {
                if (tick > 100) {
                    throw new AssertionError("The server didn't set the frequency on the held portable teleporter");
                }
                context.waitTick();
            }
            //The frequency gets synced back to the client's item
            context.waitFor(client -> FREQUENCY.equals(frequencyName(client.player.getMainHandItem())));
            context.setScreen(() -> null);
            context.waitTicks(5);

            checkBlockEntityUpdateTags(context, singleplayer);
        }
    }

    /**
     * The update tags of the block entities in a chunk sent to the client are handled by the block entity (IBlockEntityExtension#handleUpdateTag, which
     * Mekanism uses to read its update tag): a filled fluid tank in a chunk the client loads has its fluid and its fluid scale (only read by
     * handleUpdateTag, vanilla's loading of the tag as saved data wouldn't set it) on the client. The tank is filled far from the player, and the scale
     * has settled before the player gets there, so Mekanism's own update packets don't sync it.
     */
    private static void checkBlockEntityUpdateTags(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        BlockPos tankPos = singleplayer.getServer().computeOnServer(server -> {
            ServerLevel level = server.overworld();
            BlockPos pos = server.getPlayerList().getPlayers().getFirst().blockPosition().offset(512, 0, 0);
            level.setChunkForced(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), true);
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, MekanismBlocks.BASIC_FLUID_TANK.defaultState());
            ResourceHandler<FluidResource> fluids = Capabilities.Fluid.BLOCK.getCapability(level, pos, null, null, Direction.UP);
            if (fluids == null) {
                throw new AssertionError("No fluid handler for the basic fluid tank");
            }
            try (Transaction transaction = Transaction.openRoot()) {
                if (fluids.insert(FluidResource.of(Fluids.LAVA), 4_000, transaction) != 4_000) {
                    throw new AssertionError("Couldn't fill the basic fluid tank");
                }
                transaction.commit();
            }
            return pos;
        });
        //Wait for the tank's scale to settle on the server
        float scale = 0;
        for (int tick = 0; tick < 200; tick++) {
            context.waitTick();
            float current = singleplayer.getServer().computeOnServer(server -> server.overworld().getBlockEntity(tankPos) instanceof TileEntityFluidTank tank
                                                                               ? Float.valueOf(tank.prevScale) : Float.valueOf(-1));
            if (current > 0 && current == scale) {
                break;
            }
            scale = current;
        }
        if (scale <= 0) {
            throw new AssertionError("The fluid tank's scale didn't settle on the server");
        }
        context.waitTicks(20);
        singleplayer.getServer().runCommand("tp @p " + tankPos.getX() + " " + (tankPos.getY() + 1) + " " + (tankPos.getZ() + 3));
        float expectedScale = scale;
        for (int tick = 0; !context.computeOnClient(client -> client.level.getBlockEntity(tankPos) instanceof TileEntityFluidTank tank
                                                                && tank.fluidTank.resource().is(Fluids.LAVA) && tank.fluidTank.amountAsLong() == 4_000
                                                                && tank.prevScale == expectedScale); tick++) {
            if (tick > 200) {
                throw new AssertionError("The fluid tank's update tag wasn't handled on the client, it has "
                                         + context.computeOnClient(client -> client.level.getBlockEntity(tankPos) instanceof TileEntityFluidTank tank
                                                                             ? tank.fluidTank.amountAsLong() + " of " + tank.fluidTank.resource() + " with scale "
                                                                               + tank.prevScale + " instead of " + expectedScale : "no tank"));
            }
            context.waitTick();
        }
        singleplayer.getServer().runOnServer(server -> {
            server.overworld().removeBlock(tankPos, false);
            server.overworld().setChunkForced(SectionPos.blockToSectionCoord(tankPos.getX()), SectionPos.blockToSectionCoord(tankPos.getZ()), false);
        });
    }

    @Nullable
    private static String frequencyName(ItemStack stack) {
        FrequencyAware<?> frequencyAware = stack.get(MekanismDataComponents.TELEPORTER_FREQUENCY);
        return frequencyAware == null ? null : frequencyAware.identity().map(identity -> identity.key().toString()).orElse(null);
    }
}
