package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code ModelDiscovery.ModelWrapper#bakeTopGeometry} (the resolved model of every model json) passes the model's additional properties (root
 * transform, part visibility) to the geometry. Without this any caller of {@link ResolvedModel#bakeTopGeometry} that isn't redirected separately (such as
 * the children of NeoForge's composite model, or Mekanism's standalone models) loses for example the root transform.
 */
@Mixin(targets = "net.minecraft.client.resources.model.ModelDiscovery$ModelWrapper")
public abstract class ModelWrapperMixin {

    private static final String BAKE = "Lnet/minecraft/client/resources/model/geometry/UnbakedGeometry;bake(Lnet/minecraft/client/resources/model/sprite/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/minecraft/client/resources/model/ModelDebugName;)Lnet/minecraft/client/resources/model/geometry/QuadCollection;";

    @WrapOperation(method = {"bakeDefaultState", "lambda$bakeTopGeometry$0"}, at = @At(value = "INVOKE", target = BAKE), require = 2)
    private QuadCollection mekanism$bakeWithProperties(UnbakedGeometry geometry, TextureSlots textureSlots, ModelBaker baker, ModelState state,
          ModelDebugName name, Operation<QuadCollection> original) {
        return geometry.bake(textureSlots, baker, state, name, ((ResolvedModel) (Object) this).getTopAdditionalProperties());
    }
}
