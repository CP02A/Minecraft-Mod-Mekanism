package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's sound engine load and play sound events.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Shadow
    private boolean loaded;

    @Inject(method = {"<init>", "reload"}, at = @At("TAIL"))
    private void mekanism$loaded(CallbackInfo ci) {
        ModLoader.postEvent(new SoundEngineLoadEvent((SoundEngine) (Object) this));
    }

    @WrapMethod(method = "play")
    private SoundEngine.PlayResult mekanism$play(SoundInstance instance, Operation<SoundEngine.PlayResult> original) {
        if (loaded) {
            instance = ClientHooks.playSound((SoundEngine) (Object) this, instance);
            if (instance == null) {
                return SoundEngine.PlayResult.NOT_STARTED;
            }
        }
        return original.call(instance);
    }
}
