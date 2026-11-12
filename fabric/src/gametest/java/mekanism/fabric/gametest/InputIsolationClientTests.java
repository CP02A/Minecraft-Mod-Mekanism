package mekanism.fabric.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLScancode;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.MemoryUtil;

/**
 * The user's real keyboard and mouse can't influence client game tests, while the tests' simulated input (Fabric's TestInput, which calls the keyboard and
 * mouse handlers directly) still works. Real input is simulated by pushing SDL events into SDL's event queue, where the game's event loop
 * (SDLEventHandler#pollEvents, the same path as events from the OS) picks them up: key presses (forward, escape), mouse motion, a mouse click, scrolling,
 * typed text and the window losing focus (which would open the pause menu). Also checks that the real cursor position doesn't replace the simulated one
 * when the game resyncs the mouse position (MouseHandlerMixin in this test mod).
 */
public class InputIsolationClientTests implements FabricClientGameTest {

    private static final double CURSOR_X = 123.25;
    private static final double CURSOR_Y = 77.75;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            //Face south with nothing in the way
            singleplayer.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ 0 0");
            singleplayer.getServer().runCommand("execute as @p at @s run fill ~-1 ~ ~ ~1 ~2 ~8 minecraft:air");
            context.setScreen(() -> null);
            context.waitTicks(10);
            //Grabbing the mouse makes MouseHandler skip the next cursor move; use that up, so that real mouse motion would turn the camera if it got through
            context.getInput().moveCursor(1, 0);
            context.waitTicks(2);
            context.runOnClient(client -> check(client.mouseHandler.isMouseGrabbed(), "The mouse isn't grabbed in game"));

            //Real input in game: walking, the pause menu, turning, attacking, hotbar scrolling and pausing on focus loss must all be ignored
            PlayerState before = context.computeOnClient(PlayerState::of);
            ByteBuffer realText = MemoryUtil.memUTF8("real");
            try {
                context.runOnClient(client -> {
                    int window = SDLVideo.SDL_GetWindowID(client.getWindow().handle());
                    pushKey(window, SDLEvents.SDL_EVENT_KEY_DOWN, SDLScancode.SDL_SCANCODE_W, SDLKeycode.SDLK_W);
                    pushKey(window, SDLEvents.SDL_EVENT_KEY_DOWN, SDLScancode.SDL_SCANCODE_ESCAPE, SDLKeycode.SDLK_ESCAPE);
                    pushKey(window, SDLEvents.SDL_EVENT_KEY_UP, SDLScancode.SDL_SCANCODE_ESCAPE, SDLKeycode.SDLK_ESCAPE);
                    push(SDLEvents.SDL_EVENT_MOUSE_MOTION, event -> event.motion().windowID(window).x(400).y(300).xrel(300).yrel(150));
                    push(SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN, event -> event.button().windowID(window).button((byte) SDLMouse.SDL_BUTTON_LEFT).down(true)
                          .clicks((byte) 1).x(400).y(300));
                    push(SDLEvents.SDL_EVENT_MOUSE_WHEEL, event -> event.wheel().windowID(window).x(0).y(3).direction(SDLMouse.SDL_MOUSEWHEEL_NORMAL));
                    push(SDLEvents.SDL_EVENT_TEXT_INPUT, event -> event.text().windowID(window).text(realText));
                    push(SDLEvents.SDL_EVENT_WINDOW_MOUSE_LEAVE, event -> event.window().windowID(window));
                    push(SDLEvents.SDL_EVENT_WINDOW_FOCUS_LOST, event -> event.window().windowID(window));
                });
                context.waitTicks(10);
            } finally {
                MemoryUtil.memFree(realText);
            }
            context.runOnClient(client -> {
                assertEventsConsumed();
                check(client.gui.screen() == null, "Real input opened a screen: " + client.gui.screen());
                check(client.isWindowActive(), "The window lost focus from a real focus event");
                check(!client.options.keyUp.isDown(), "A real key press held the forward key");
                check(!client.mouseHandler.isLeftPressed(), "A real mouse click pressed the left mouse button");
                PlayerState after = PlayerState.of(client);
                check(after.position.distanceTo(before.position) < 0.01, "Real input moved the player from " + before.position + " to " + after.position);
                check(after.yRot == before.yRot && after.xRot == before.xRot, "Real mouse motion turned the camera from " + before + " to " + after);
                check(after.slot == before.slot, "Real scrolling changed the selected hotbar slot from " + before.slot + " to " + after.slot);
            });

            //Simulated input in game still works
            context.getInput().holdKeyFor(options -> options.keyUp, 10);
            context.waitTicks(2);
            PlayerState walked = context.computeOnClient(PlayerState::of);
            check(walked.position.distanceTo(before.position) > 0.5, "Holding the forward key with TestInput didn't move the player: " + walked.position);
            context.getInput().moveCursor(300, 0);
            context.waitTicks(2);
            PlayerState turned = context.computeOnClient(PlayerState::of);
            check(turned.yRot != walked.yRot, "Moving the cursor with TestInput didn't turn the camera");
            context.getInput().scroll(-1);
            context.waitTicks(2);
            PlayerState scrolled = context.computeOnClient(PlayerState::of);
            check(scrolled.slot != turned.slot, "Scrolling with TestInput didn't change the selected hotbar slot");

            //Real typing in a text box is ignored, simulated typing works, and resyncing the mouse position keeps the simulated cursor position
            context.setScreen(() -> new ChatScreen("", false));
            context.waitTicks(2);
            ByteBuffer typed = MemoryUtil.memUTF8("real");
            try {
                context.runOnClient(client -> {
                    int window = SDLVideo.SDL_GetWindowID(client.getWindow().handle());
                    push(SDLEvents.SDL_EVENT_TEXT_INPUT, event -> event.text().windowID(window).text(typed));
                    pushKey(window, SDLEvents.SDL_EVENT_KEY_DOWN, SDLScancode.SDL_SCANCODE_ESCAPE, SDLKeycode.SDLK_ESCAPE);
                });
                context.waitTicks(5);
            } finally {
                MemoryUtil.memFree(typed);
            }
            context.runOnClient(client -> {
                assertEventsConsumed();
                check(client.gui.screen() instanceof ChatScreen, "A real escape key press closed the chat screen");
                check(chatValue(client).isEmpty(), "Real typing reached the chat box: " + chatValue(client));
            });
            context.getInput().typeChars("sim");
            context.waitTick();
            context.runOnClient(client -> check("sim".equals(chatValue(client)), "Typing with TestInput didn't reach the chat box: " + chatValue(client)));
            context.getInput().setCursorPos(CURSOR_X, CURSOR_Y);
            context.runOnClient(client -> {
                //Called by Minecraft#resizeGui (window resizes, GUI scale changes); it would read the user's real cursor position
                client.mouseHandler.resyncMousePosition();
                check(client.mouseHandler.xpos() == CURSOR_X && client.mouseHandler.ypos() == CURSOR_Y,
                      "Resyncing the mouse position replaced the simulated cursor position with " + client.mouseHandler.xpos() + ", " + client.mouseHandler.ypos());
            });
            context.setScreen(() -> null);
            context.waitTicks(2);

            //A simulated escape key press still opens the pause menu
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitFor(client -> client.gui.screen() instanceof PauseScreen);
            context.setScreen(() -> null);
            context.waitTicks(2);
        }
    }

    private static String chatValue(Minecraft client) {
        Screen screen = client.gui.screen();
        return screen != null && screen.getFocused() instanceof EditBox box ? box.getValue() : "<no focused text box>";
    }

    private static void pushKey(int window, int type, int scancode, int key) {
        push(type, event -> event.key().windowID(window).scancode(scancode).key(key).down(type == SDLEvents.SDL_EVENT_KEY_DOWN));
    }

    private static void push(int type, Consumer<SDL_Event> setup) {
        SDL_Event event = SDL_Event.calloc();
        try {
            event.type(type);
            setup.accept(event);
            check(SDLEvents.SDL_PushEvent(event), "Couldn't push an SDL event of type " + type);
        } finally {
            event.free();
        }
    }

    /**
     * Makes sure the pushed events went through the game's event loop (and were ignored there), rather than still waiting in SDL's queue.
     */
    private static void assertEventsConsumed() {
        check(!SDLEvents.SDL_HasEvents(SDLEvents.SDL_EVENT_KEY_DOWN, SDLEvents.SDL_EVENT_TEXT_INPUT), "The pushed key/text events weren't polled");
        check(!SDLEvents.SDL_HasEvents(SDLEvents.SDL_EVENT_MOUSE_MOTION, SDLEvents.SDL_EVENT_MOUSE_WHEEL), "The pushed mouse events weren't polled");
        check(!SDLEvents.SDL_HasEvents(SDLEvents.SDL_EVENT_WINDOW_FIRST, SDLEvents.SDL_EVENT_WINDOW_LAST), "The pushed window events weren't polled");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record PlayerState(Vec3 position, float yRot, float xRot, int slot) {

        private static PlayerState of(Minecraft client) {
            return new PlayerState(client.player.position(), client.player.getYRot(), client.player.getXRot(), client.player.getInventory().getSelectedSlot());
        }
    }
}
