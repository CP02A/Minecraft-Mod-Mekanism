package mekanism.fabric.mixin.client;

import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemModelGenerator.class)
public interface ItemModelGeneratorAccessor {

    @Invoker("bakeExtrudedSprite")
    static void mekanism$bakeExtrudedSprite(QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, BakedQuad.MaterialInfo materialInfo) {
        throw new AssertionError();
    }
}
