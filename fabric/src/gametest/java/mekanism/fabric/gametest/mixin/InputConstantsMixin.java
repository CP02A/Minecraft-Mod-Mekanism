package mekanism.fabric.gametest.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Never lets the client game test window capture the mouse: {@link InputConstants#grabMouse} switches the window to relative mouse mode (hidden,
 * captured cursor) and both it and {@link InputConstants#releaseMouse} warp the real cursor into the window. Fabric's client game test API cancels these
 * as well; this keeps it guaranteed for Mekanism's UI tests even if that changes. MouseHandler#isMouseGrabbed still reports the game's state, so tests
 * behave the same. Only part of the test mod (gametest source set, client game test runs only).
 */
@Mixin(InputConstants.class)
public class InputConstantsMixin {

    @Unique
    private static final boolean CLIENT_GAME_TEST = System.getProperty("fabric.client.gametest") != null;

    @Inject(method = {"grabMouse", "releaseMouse"}, at = @At("HEAD"), cancellable = true)
    private static void mekanism_tests$neverCaptureCursor(CallbackInfo ci) {
        if (CLIENT_GAME_TEST) {
            ci.cancel();
        }
    }
}
