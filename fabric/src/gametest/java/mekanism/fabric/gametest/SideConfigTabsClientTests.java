package mekanism.fabric.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mekanism.api.RelativeSide;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.button.SideDataButton;
import mekanism.client.gui.element.tab.GuiConfigTypeTab;
import mekanism.client.gui.element.tab.window.GuiSideConfigurationTab;
import mekanism.client.gui.element.window.GuiSideConfiguration;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Clicks through the side configuration window's transmission type tabs for a machine with two types and one with four. Each type gets selected
 * once, then the selected tab is clicked again (no change) and the first type is selected again (going back). After each click it checks the
 * window's state (selected type, every tab shown with only the selected one pressed, the eject and side buttons) and takes a screenshot (in the run
 * directory's screenshots folder).
 */
public class SideConfigTabsClientTests implements FabricClientGameTest {

    private static final List<String> MACHINES = List.of(
          //Item, energy
          "mekanism:ultimate_smelting_factory",
          //Item, fluid, chemical, energy
          "mekanism:pressurized_reaction_chamber"
    );

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            singleplayer.getServer().runCommand("time set day");
            BlockPos playerPos = singleplayer.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
            for (String machine : MACHINES) {
                checkMachine(context, singleplayer, playerPos, machine);
            }
        }
    }

    private static void checkMachine(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos playerPos, String machine) {
        String name = machine.substring(machine.indexOf(':') + 1);
        BlockPos pos = playerPos.offset(2, 1, 0);
        singleplayer.getServer().runCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + machine + "[facing=west] replace");
        //Apply the item's default side configuration like placing it would, setblock leaves all sides disabled
        singleplayer.getServer().runOnServer(server -> server.overworld().getBlockEntity(pos)
              .applyComponentsFromItemStack(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(machine)))));
        singleplayer.getServer().runCommand("tp @p " + playerPos.getX() + " " + playerPos.getY() + " " + playerPos.getZ() + " -90 20");
        context.waitTicks(20);
        singleplayer.getConnection().waitForChunksRender();
        context.getInput().pressKey(options -> options.keyUse);
        context.waitFor(client -> client.gui.screen() instanceof GuiMekanismTile<?, ?> gui && gui.getTileEntity().getBlockPos().equals(pos));
        context.waitTicks(5);

        clickElement(context, context.computeOnClient(client -> findElement(client.gui.screen().children(), GuiSideConfigurationTab.class)));
        context.waitFor(client -> sideConfigWindow(client) != null);
        context.waitTicks(5);

        List<TransmissionType> types = context.computeOnClient(client -> List.copyOf(((ISideConfiguration) ((GuiMekanismTile<?, ?>) client.gui.screen())
              .getTileEntity()).getConfig().getTransmissions()));
        if (types.size() < 2) {
            throw new AssertionError(machine + " has fewer than two transmission types: " + types);
        }
        List<String> failures = new ArrayList<>();
        //The window opens on the first type
        checkState(context, machine, types.getFirst(), types, failures);
        context.takeScreenshot("mekanism-side-config-tabs-" + name + "-initial-" + lower(types.getFirst()));
        //Select every other type in order, click the selected tab again, then go back to the first type
        List<TransmissionType> clicks = new ArrayList<>(types.subList(1, types.size()));
        clicks.add(types.getLast());
        clicks.add(types.getFirst());
        TransmissionType from = types.getFirst();
        for (TransmissionType to : clicks) {
            clickTab(context, to);
            checkState(context, machine + " (" + from + " -> " + to + ")", to, types, failures);
            context.takeScreenshot("mekanism-side-config-tabs-" + name + "-" + lower(from) + "-to-" + lower(to));
            from = to;
        }
        context.setScreen(() -> null);
        context.waitTicks(5);
        if (!failures.isEmpty()) {
            throw new AssertionError("Side config tab problems:\n  " + String.join("\n  ", failures));
        }
    }

    private static String lower(TransmissionType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private static void clickTab(ClientGameTestContext context, TransmissionType type) {
        GuiConfigTypeTab tab = context.computeOnClient(client -> {
            GuiSideConfiguration<?> window = sideConfigWindow(client);
            if (window == null) {
                throw new AssertionError("The side config window closed");
            }
            return window.children().stream().filter(GuiConfigTypeTab.class::isInstance).map(GuiConfigTypeTab.class::cast)
                  .filter(t -> t.getTransmissionType() == type).findFirst().orElseThrow(() -> new AssertionError("No tab for " + type));
        });
        clickElement(context, tab);
        context.waitTicks(3);
    }

    /**
     * Checks the side config window after selecting the given type, adding any problems to the failures (so all combinations get checked and screenshotted).
     */
    private static void checkState(ClientGameTestContext context, String what, TransmissionType expected, List<TransmissionType> types, List<String> failures) {
        List<String> problems = context.computeOnClient(client -> {
            List<String> result = new ArrayList<>();
            GuiMekanism<?> gui = (GuiMekanism<?>) client.gui.screen();
            long windows = gui.getWindows().stream().filter(GuiSideConfiguration.class::isInstance).count();
            if (windows != 1) {
                result.add("expected one side config window, found " + windows);
                return result;
            }
            GuiSideConfiguration<?> window = sideConfigWindow(client);
            TransmissionType current = currentType(window);
            if (current != expected) {
                result.add("selected type is " + current + " instead of " + expected);
            }
            List<GuiConfigTypeTab> tabs = window.children().stream().filter(GuiConfigTypeTab.class::isInstance).map(GuiConfigTypeTab.class::cast).toList();
            if (tabs.size() != types.size()) {
                result.add("expected " + types.size() + " tabs, found " + tabs.size());
            }
            for (TransmissionType type : types) {
                List<GuiConfigTypeTab> matching = tabs.stream().filter(t -> t.getTransmissionType() == type).toList();
                if (matching.size() != 1) {
                    result.add("expected one " + type + " tab, found " + matching.size());
                    continue;
                }
                GuiConfigTypeTab tab = matching.getFirst();
                if (!tab.visible || !tab.active) {
                    result.add("the " + type + " tab is " + (tab.visible ? "inactive" : "hidden") + " while " + current + " is selected");
                }
                if (isPressed(tab) != (type == current)) {
                    result.add("the " + type + " tab is " + (isPressed(tab) ? "pressed" : "not pressed") + " while " + current + " is selected");
                }
                if ((tab.getX() < 0 || tab.getY() < 0 || tab.getRight() > gui.width || tab.getBottom() > gui.height)) {
                    result.add("the " + type + " tab is off screen at " + tab.getX() + ", " + tab.getY());
                }
            }
            //Tabs must not overlap each other
            for (int i = 0; i < tabs.size(); i++) {
                for (int j = i + 1; j < tabs.size(); j++) {
                    if (tabs.get(i).getRectangle().intersects(tabs.get(j).getRectangle())) {
                        result.add("the " + tabs.get(i).getTransmissionType() + " and " + tabs.get(j).getTransmissionType() + " tabs overlap");
                    }
                }
            }
            //The eject and side buttons follow the selected type's config
            ConfigInfo info = ((ISideConfiguration) ((GuiMekanismTile<?, ?>) gui).getTileEntity()).getConfig().getConfig(current);
            if (info == null) {
                result.add("no config for " + current);
                return result;
            }
            MekanismImageButton eject = window.children().stream().filter(c -> c.getClass() == MekanismImageButton.class).map(MekanismImageButton.class::cast)
                  .findFirst().orElse(null);
            if (eject == null) {
                result.add("no eject button");
            } else if (eject.active != info.canEject()) {
                result.add("the eject button is " + (eject.active ? "active" : "inactive") + " but canEject is " + info.canEject());
            }
            for (Map.Entry<RelativeSide, SideDataButton> entry : sideButtons(window).entrySet()) {
                if (entry.getValue().active != info.isSideEnabled(entry.getKey())) {
                    result.add("the " + entry.getKey() + " side button is " + (entry.getValue().active ? "active" : "inactive") + " but the side is "
                               + (info.isSideEnabled(entry.getKey()) ? "enabled" : "disabled"));
                }
            }
            return result;
        });
        for (String problem : problems) {
            failures.add(what + ": " + problem);
        }
    }

    private static GuiSideConfiguration<?> sideConfigWindow(Minecraft client) {
        if (client.gui.screen() instanceof GuiMekanism<?> gui) {
            for (GuiWindow window : gui.getWindows()) {
                if (window instanceof GuiSideConfiguration<?> config) {
                    return config;
                }
            }
        }
        return null;
    }

    private static TransmissionType currentType(GuiSideConfiguration<?> window) {
        return window.getCurrentType();
    }

    private static boolean isPressed(GuiConfigTypeTab tab) {
        try {
            Method method = GuiConfigTypeTab.class.getDeclaredMethod("isPressed");
            method.setAccessible(true);
            return (boolean) method.invoke(tab);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<RelativeSide, SideDataButton> sideButtons(GuiSideConfiguration<?> window) {
        try {
            Field field = GuiSideConfiguration.class.getDeclaredField("sideConfigButtons");
            field.setAccessible(true);
            return (Map<RelativeSide, SideDataButton>) field.get(window);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static <T extends GuiEventListener> T findElement(List<? extends GuiEventListener> elements, Class<T> type) {
        return elements.stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow(() -> new AssertionError("No " + type.getSimpleName()));
    }

    private static void clickElement(ClientGameTestContext context, AbstractWidget widget) {
        double scale = context.computeOnClient(client -> client.getWindow().getGuiScale());
        context.getInput().setCursorPos((widget.getX() + widget.getWidth() / 2.0) * scale, (widget.getY() + widget.getHeight() / 2.0) * scale);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }
}
