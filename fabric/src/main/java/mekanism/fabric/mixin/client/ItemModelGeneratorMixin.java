package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mekanism.fabric.shim.client.ItemLayerFaceData;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Applies NeoForge's item layer face data to generated item models, see {@link ItemLayerFaceData}.
 */
@Mixin(ItemModelGenerator.class)
public abstract class ItemModelGeneratorMixin {

    @ModifyReturnValue(method = "geometry", at = @At("RETURN"))
    private UnbakedGeometry mekanism$bakeWithFaceData(UnbakedGeometry original) {
        return ItemLayerFaceData.wrap(original);
    }
}
