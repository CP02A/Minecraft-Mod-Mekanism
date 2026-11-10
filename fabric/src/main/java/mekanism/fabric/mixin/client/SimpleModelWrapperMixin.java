package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mekanism.fabric.shim.client.ModelPropertiesBridge;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code ResolvedModel#bakeTopGeometry} passes the model's additional properties (root transform, part visibility) to the geometry. As
 * vanilla's version is an interface default method, its callers are redirected instead.
 */
@Mixin(SimpleModelWrapper.class)
public abstract class SimpleModelWrapperMixin {

    @WrapOperation(method = "bake", at = @At(value = "INVOKE", target = ModelPropertiesBridge.BAKE_TOP_GEOMETRY))
    private static QuadCollection mekanism$bakeWithProperties(ResolvedModel model, TextureSlots textureSlots, ModelBaker baker, ModelState state,
          Operation<QuadCollection> original) {
        return ModelPropertiesBridge.bakeTopGeometry(model, textureSlots, baker, state);
    }
}
