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
import net.minecraft.world.item.ItemStack;
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
        }
    }

    @Nullable
    private static String frequencyName(ItemStack stack) {
        FrequencyAware<?> frequencyAware = stack.get(MekanismDataComponents.TELEPORTER_FREQUENCY);
        return frequencyAware == null ? null : frequencyAware.identity().map(identity -> identity.key().toString()).orElse(null);
    }
}
