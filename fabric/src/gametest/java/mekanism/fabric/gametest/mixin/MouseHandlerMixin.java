package mekanism.fabric.gametest.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the user's real mouse out of client game tests. Fabric's client game test API already drops the real keyboard, mouse, text, scroll, drop and
 * window focus/enter/resize events (its SDLEventHandler and Window mixins) and never grabs the cursor, but {@link MouseHandler#resyncMousePosition()}
 * (called by Minecraft#resizeGui at startup, on window and GUI scale changes) still reads the real cursor position from SDL and overwrites the test's
 * simulated cursor position with it, which changes what a GUI shows as hovered. Only part of the test mod (gametest source set, client game test runs).
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    private static final boolean CLIENT_GAME_TEST = System.getProperty("fabric.client.gametest") != null;

    @Inject(method = "resyncMousePosition", at = @At("HEAD"), cancellable = true)
    private void mekanism_tests$ignoreRealCursorPosition(CallbackInfo ci) {
        if (CLIENT_GAME_TEST) {
            ci.cancel();
        }
    }
}
