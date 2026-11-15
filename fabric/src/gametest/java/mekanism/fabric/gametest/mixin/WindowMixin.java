package mekanism.fabric.gametest.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the client game test window out of the user's way, so using the PC while the UI tests run can't disturb them (and the tests can't disturb the
 * user): the window opens without taking the focus, can't be focused by clicking it (so real keyboard input never goes to it), and never confines the
 * cursor ({@code SDL_SetWindowMouseGrab}, which vanilla only uses in exclusive fullscreen). Only part of the test mod (gametest source set, client game
 * test runs only).
 */
@Mixin(Window.class)
public class WindowMixin {

    @Unique
    private static final boolean CLIENT_GAME_TEST = System.getProperty("fabric.client.gametest") != null;

    @Inject(method = "createWindow", at = @At("HEAD"))
    private void mekanism_tests$dontActivateWhenShown(CallbackInfoReturnable<Long> cir) {
        if (CLIENT_GAME_TEST) {
            SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOW_ACTIVATE_WHEN_SHOWN, "0");
            SDLHints.SDL_SetHint(SDLHints.SDL_HINT_WINDOW_ACTIVATE_WHEN_RAISED, "0");
        }
    }

    @ModifyArg(method = "createWindow", at = @At(value = "INVOKE",
          target = "Lcom/mojang/renderpearl/api/device/GpuBackend;createWindow(Ljava/lang/String;IIJ)J"), index = 3)
    private long mekanism_tests$notFocusable(long flags) {
        return CLIENT_GAME_TEST ? flags | SDLVideo.SDL_WINDOW_NOT_FOCUSABLE : flags;
    }

    @Inject(method = "updateWindowMouseGrab", at = @At("HEAD"), cancellable = true)
    private void mekanism_tests$neverConfineCursor(CallbackInfo ci) {
        if (CLIENT_GAME_TEST) {
            ci.cancel();
        }
    }
}
