package mekanism.fabric.mixin.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets unbaked models resolve additional dependencies (NeoForge's {@code UnbakedModelExtension#resolveDependencies}), used by custom models with
 * children such as NeoForge's composite model.
 */
@Mixin(ModelDiscovery.class)
public abstract class ModelDiscoveryMixin {

    @Shadow
    @Final
    private ResolvableModel.Resolver resolver;
    @Shadow
    @Final
    private Object2ObjectMap<Identifier, Object> modelWrappers;
    @Shadow
    @Final
    private Object2ObjectFunction<Identifier, Object> uncachedResolver;

    /**
     * Like NeoForge, avoid {@code computeIfAbsent}, as resolving the dependencies of a model can discover further models (nested modification of the map).
     */
    @Inject(method = "getOrCreateModel", at = @At("HEAD"), cancellable = true)
    private void mekanism$getOrCreateModel(Identifier id, CallbackInfoReturnable<Object> cir) {
        Object wrapper = modelWrappers.get(id);
        if (wrapper == null) {
            wrapper = uncachedResolver.get(id);
            modelWrappers.put(id, wrapper);
        }
        cir.setReturnValue(wrapper);
    }

    @Inject(method = "createAndQueueWrapper", at = @At("RETURN"))
    private void mekanism$resolveDependencies(Identifier id, UnbakedModel rawModel, CallbackInfoReturnable<?> cir) {
        rawModel.resolveDependencies(this.resolver);
    }
}
