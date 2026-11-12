package mekanism.fabric.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterStack;
import mekanism.common.tier.TransporterTier;
import mekanism.common.tile.transmitter.TileEntityLogisticalTransporterBase;
import mekanism.common.util.TransporterUtils;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import mekanism.shim.neoforged.neoforge.transfer.item.ItemResource;

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
            //Diagonal view (looking north-east), where every item should still face the camera
            view(context, singleplayer, lineX, lineZ, x - 2.5, origin.getY(), z + 1.5, 225, "wide-diagonal-looking-north-east");
            //Handoffs between transporters: the rendered position of a single stack must move forward smoothly, without snapping back or jumping ahead.
            // Both directions, as which transporter ticks first decides whether the server moves a stack again in the tick it enters a transporter
            // (blocks placed by fill tick from west to east)
            assertSmoothHandoffs(context, singleplayer, new BlockPos(x - 4, y, z + 4), Direction.EAST);
            assertSmoothHandoffs(context, singleplayer, new BlockPos(x + 3, y, z + 7), Direction.WEST);
        }
    }

    private static final int HANDOFF_LINE_LENGTH = 8;

    /**
     * Sends a single stack down a line of basic transporters starting at {@code start}, sampling its rendered position (along the line, at the start and the middle
     * of each tick) until it arrives, and checks that it never moves backwards or faster than the catch-up after a handoff allows.
     */
    private static void assertSmoothHandoffs(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos start, Direction direction) {
        BlockPos end = start.relative(direction, HANDOFF_LINE_LENGTH - 1);
        command(singleplayer, "fill %d %d %d %d %d %d mekanism:basic_logistical_transporter", start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
        BlockPos source = start.relative(direction.getOpposite());
        BlockPos destination = end.relative(direction);
        command(singleplayer, "setblock %d %d %d minecraft:chest", source.getX(), source.getY(), source.getZ());
        command(singleplayer, "setblock %d %d %d minecraft:chest", destination.getX(), destination.getY(), destination.getZ());
        singleplayer.getConnection().waitForChunksRender();
        context.waitTicks(5);
        send(singleplayer, start, source, Items.RAW_IRON, 1);
        List<Float> samples = new ArrayList<>();
        //Basic transporters move 5% of a block per tick, so the stack takes 20 ticks per transporter
        float normalHalfTickStep = 0.5F * TransporterTier.BASIC.getSpeed() / 100F;
        boolean seen = false;
        for (int tick = 0; tick < 40 * HANDOFF_LINE_LENGTH; tick++) {
            context.waitTick();
            float[] positions = context.computeOnClient(client -> {
                float[] result = {Float.NaN, Float.NaN};
                for (int i = 0; i < HANDOFF_LINE_LENGTH; i++) {
                    BlockPos pos = start.relative(direction, i);
                    if (client.level.getBlockEntity(pos) instanceof TileEntityLogisticalTransporterBase tile) {
                        LogisticalTransporterBase transporter = tile.getTransmitter();
                        for (TransporterStack stack : transporter.getTransit()) {
                            for (int half = 0; half < 2; half++) {
                                Vector3f stackPos = TransporterUtils.getStackPosition(transporter, stack, 0.5F * half * transporter.tier.getSpeed());
                                //Distance along the line from the start transporter's center
                                float along = direction.getStepX() * (pos.getX() + stackPos.x() - start.getX() - 0.5F)
                                              + direction.getStepZ() * (pos.getZ() + stackPos.z() - start.getZ() - 0.5F);
                                //Note: Right at a handoff, the stack can briefly be in both transporters; take the one furthest along
                                result[half] = Float.isNaN(result[half]) ? along : Math.max(result[half], along);
                            }
                        }
                    }
                }
                return result;
            });
            if (Float.isNaN(positions[0])) {
                if (seen) {
                    //Arrived in the destination
                    break;
                }
                continue;
            }
            seen = true;
            samples.add(positions[0]);
            samples.add(positions[1]);
        }
        if (samples.size() < 2 * 20 * (HANDOFF_LINE_LENGTH - 1)) {
            throw new AssertionError("The stack going " + direction + " was only seen for " + samples.size() / 2 + " ticks");
        }
        for (int i = 1; i < samples.size(); i++) {
            float delta = samples.get(i) - samples.get(i - 1);
            //Catching up after a handoff moves a bit faster than normal, a skipped step would be (at least) three times as fast
            if (delta < -0.001F || delta > 1.6F * normalHalfTickStep) {
                throw new AssertionError(String.format(Locale.ROOT, "The stack going %s snapped from %.3f to %.3f blocks along the line (sample %d of %d)",
                      direction, samples.get(i - 1), samples.get(i), i, samples.size()));
            }
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
