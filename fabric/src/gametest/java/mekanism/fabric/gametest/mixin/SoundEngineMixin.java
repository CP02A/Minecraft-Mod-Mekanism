package mekanism.fabric.gametest.mixin;

import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns all sound off in client game tests, from the title screen on: the sound library (OpenAL) is never loaded, so the engine stays in vanilla's
 * "no audio device" state where every play/stop/pause call is a no-op. The audio device change check is skipped too, as it would otherwise keep trying
 * to reload the library. Only part of the test mod (gametest source set, client game test runs only).
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Unique
    private static final boolean CLIENT_GAME_TEST = System.getProperty("fabric.client.gametest") != null;

    @Inject(method = "loadLibrary", at = @At("HEAD"), cancellable = true)
    private void mekanism_tests$dontLoadSoundLibrary(CallbackInfo ci) {
        if (CLIENT_GAME_TEST) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldChangeDevice", at = @At("HEAD"), cancellable = true)
    private void mekanism_tests$dontWatchAudioDevices(CallbackInfoReturnable<Boolean> cir) {
        if (CLIENT_GAME_TEST) {
            cir.setReturnValue(false);
        }
    }
}
