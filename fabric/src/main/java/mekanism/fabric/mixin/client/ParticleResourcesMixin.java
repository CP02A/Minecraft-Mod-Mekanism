package mekanism.fabric.mixin.client;

import net.minecraft.client.particle.ParticleResources;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's {@link RegisterParticleProvidersEvent}, fired once the vanilla providers are registered.
 */
@Mixin(ParticleResources.class)
public abstract class ParticleResourcesMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mekanism$registerProviders(CallbackInfo ci) {
        ModLoader.postEvent(new RegisterParticleProvidersEvent((ParticleResources) (Object) this));
    }
}
