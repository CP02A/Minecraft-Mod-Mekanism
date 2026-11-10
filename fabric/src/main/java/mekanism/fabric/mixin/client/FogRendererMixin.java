package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's fog color and fog render events (and fluid type fog extensions).
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @WrapOperation(method = "computeFogColor", at = @At(value = "INVOKE", target = "Lorg/joml/Vector4f;set(FFFF)Lorg/joml/Vector4f;"))
    private Vector4f mekanism$fogColor(Vector4f dest, float red, float green, float blue, float alpha, Operation<Vector4f> original, Camera camera, float partialTicks,
          ClientLevel level, int renderDistance, float darkenWorldAmount) {
        ClientHooks.getFogColor(camera, partialTicks, level, renderDistance, darkenWorldAmount, red, green, blue, dest);
        return dest;
    }

    @WrapOperation(method = "setupFog", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/client/renderer/fog/environment/FogEnvironment;setupFog(Lnet/minecraft/client/renderer/fog/FogData;Lnet/minecraft/client/Camera;Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/DeltaTracker;)V"))
    private void mekanism$captureEnvironment(FogEnvironment environment, FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker,
          Operation<Void> original, @Share("environment") LocalRef<FogEnvironment> chosenEnvironment) {
        original.call(environment, fog, camera, level, renderDistance, deltaTracker);
        chosenEnvironment.set(environment);
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void mekanism$setupFog(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level,
          CallbackInfoReturnable<FogData> cir, @Share("environment") LocalRef<FogEnvironment> chosenEnvironment) {
        ClientHooks.onSetupFog(chosenEnvironment.get(), getFogType(camera), camera, deltaTracker.getGameTimeDeltaPartialTick(false), renderDistanceInChunks,
              cir.getReturnValue());
    }

    @Shadow
    protected abstract FogType getFogType(Camera camera);
}
