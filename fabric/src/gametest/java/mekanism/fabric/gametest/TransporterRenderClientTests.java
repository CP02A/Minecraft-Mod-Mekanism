package mekanism.fabric.gametest;

import java.util.Locale;
import mekanism.common.tile.transmitter.TileEntityLogisticalTransporterBase;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Client game test for the logistical transporter's item rendering: sends stacks of different sizes of flat items down two lines of transporters (one along the x
 * axis, one along the z axis) and takes screenshots of them from all four horizontal directions (in the run directory's screenshots folder).
 */
public class TransporterRenderClientTests implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            BlockPos origin = context.computeOnClient(client -> client.player.blockPosition());
            int x = origin.getX(), y = origin.getY() + 1, z = origin.getZ();
            singleplayer.getServer().runCommand("time set day");
            //Line along the x axis (items moving east) and a line along the z axis (items moving south), each between two chests
            command(singleplayer, "fill %d %d %d %d %d %d mekanism:basic_logistical_transporter", x - 5, y, z - 2, x + 5, y, z - 2);
            command(singleplayer, "setblock %d %d %d minecraft:chest", x - 6, y, z - 2);
            command(singleplayer, "setblock %d %d %d minecraft:chest", x + 6, y, z - 2);
            command(singleplayer, "fill %d %d %d %d %d %d mekanism:basic_logistical_transporter", x - 9, y, z - 5, x - 9, y, z + 5);
            command(singleplayer, "setblock %d %d %d minecraft:chest", x - 9, y, z - 6);
            command(singleplayer, "setblock %d %d %d minecraft:chest", x - 9, y, z + 6);
            context.runOnClient(client -> client.options.fov().set(30));
            BlockPos lineX = new BlockPos(x - 5, y, z - 2);
            BlockPos lineZ = new BlockPos(x - 9, y, z - 5);
            //Fill both lines up
            for (int i = 0; i < 10; i++) {
                sendBoth(context, singleplayer, lineX, lineZ, i);
            }
            //Look at the middle of a line from 1.8 blocks away, from every horizontal direction
            view(context, singleplayer, lineX, lineZ, x + 0.5, origin.getY(), z - 1.5 + 1.8, 180, "south-looking-north");
            view(context, singleplayer, lineX, lineZ, x + 0.5, origin.getY(), z - 1.5 - 1.8, 0, "north-looking-south");
            view(context, singleplayer, lineX, lineZ, x - 8.5 + 1.8, origin.getY(), z + 0.5, 90, "east-looking-west");
            view(context, singleplayer, lineX, lineZ, x - 8.5 - 1.8, origin.getY(), z + 0.5, -90, "west-looking-east");
            //Wide views of whole lines, so that items are also seen at an angle
            context.runOnClient(client -> client.options.fov().set(70));
            view(context, singleplayer, lineX, lineZ, x + 0.5, origin.getY(), z - 1.5 + 2.5, 180, "wide-south-looking-north");
            view(context, singleplayer, lineX, lineZ, x - 8.5 - 2.5, origin.getY(), z + 0.5, -90, "wide-west-looking-east");
            //Diagonal view (looking north-east), where items next to each other shouldn't face different ways
            view(context, singleplayer, lineX, lineZ, x - 2.5, origin.getY(), z + 1.5, 225, "wide-diagonal-looking-north-east");
        }
    }

    private static void command(TestSingleplayerContext singleplayer, String command, Object... args) {
        singleplayer.getServer().runCommand(String.format(Locale.ROOT, command, args));
    }

    private static void view(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos lineX, BlockPos lineZ, double x, int y, double z, float yaw,
          String name) {
        command(singleplayer, "tp @p %.2f %d %.2f %.1f 15", x, y, z, yaw);
        for (int i = 0; i < 3; i++) {
            sendBoth(context, singleplayer, lineX, lineZ, i);
        }
        singleplayer.getConnection().waitForChunksRender();
        context.takeScreenshot("mekanism-transporter-" + name);
    }

    private static void sendBoth(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos lineX, BlockPos lineZ, int i) {
        Item item = i % 2 == 0 ? Items.RAW_IRON : Items.ARROW;
        int amount = i % 3 == 0 ? 64 : 2;
        send(singleplayer, lineX, lineX.west(), item, amount);
        send(singleplayer, lineZ, lineZ.north(), item, amount);
        context.waitTicks(10);
    }

    private static void send(TestSingleplayerContext singleplayer, BlockPos start, BlockPos from, Item item, int amount) {
        singleplayer.getServer().runOnServer(server -> {
            ServerLevel level = server.overworld();
            if (level.getBlockEntity(start) instanceof TileEntityLogisticalTransporterBase transporter) {
                transporter.getTransmitter().insertUnchecked(level, from.asLong(), ItemResource.of(item), amount, null);
            } else {
                throw new AssertionError("No transporter at " + start);
            }
        });
    }
}
