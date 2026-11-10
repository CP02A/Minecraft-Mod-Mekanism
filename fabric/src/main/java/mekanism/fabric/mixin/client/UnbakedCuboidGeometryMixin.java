package mekanism.fabric.mixin.client;

import java.util.List;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * NeoForge's root transform ({@code transform} key) for json model elements.
 */
@Mixin(UnbakedCuboidGeometry.class)
public abstract class UnbakedCuboidGeometryMixin {

    @Shadow
    public abstract List<CuboidModelElement> elements();

    /**
     * Overrides {@link net.neoforged.neoforge.client.extensions.UnbakedGeometryExtension#bake(TextureSlots, ModelBaker, ModelState, ModelDebugName, ContextMap)}.
     */
    public QuadCollection bake(TextureSlots textures, ModelBaker modelBaker, ModelState modelState, ModelDebugName name, ContextMap additionalProperties) {
        var transform = additionalProperties.get(NeoForgeModelProperties.TRANSFORM);
        if (transform != null) {
            modelState = UnbakedElementsHelper.composeRootTransformIntoModelState(modelState, transform);
        }
        return UnbakedCuboidGeometry.bake(elements(), textures, modelBaker, modelState, name);
    }
}
