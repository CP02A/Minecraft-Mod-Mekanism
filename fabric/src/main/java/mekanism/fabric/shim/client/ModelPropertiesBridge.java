package mekanism.fabric.shim.client;

import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Map;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import org.jspecify.annotations.Nullable;

/**
 * NeoForge's additional model properties ({@code NeoForgeModelProperties}: root transform, part visibility and item layer face data) for vanilla's
 * model baking: NeoForge's {@code ResolvedModel#bakeTopGeometry} passes the properties of the model chain to the geometry, and its {@code CuboidModel}
 * reads them from the model json.
 */
public final class ModelPropertiesBridge {

    public static final String BAKE_TOP_GEOMETRY = "Lnet/minecraft/client/resources/model/ResolvedModel;bakeTopGeometry(Lnet/minecraft/client/resources/model/sprite/TextureSlots;Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/renderer/block/dispatch/ModelState;)Lnet/minecraft/client/resources/model/geometry/QuadCollection;";

    private ModelPropertiesBridge() {
    }

    /**
     * NeoForge's version of {@link ResolvedModel#bakeTopGeometry(TextureSlots, ModelBaker, ModelState)}, which passes the model's additional properties to
     * the geometry.
     */
    public static QuadCollection bakeTopGeometry(ResolvedModel model, TextureSlots textureSlots, ModelBaker baker, ModelState state) {
        return model.getTopGeometry().bake(textureSlots, baker, state, model, model.getTopAdditionalProperties());
    }

    /**
     * Implemented on {@link net.minecraft.client.resources.model.cuboid.CuboidModel} to hold the NeoForge properties read from its json.
     */
    public interface CuboidModelProperties {

        void mekanism$setNeoForgeProperties(@Nullable Transformation rootTransform, Map<String, Boolean> partVisibility,
              Int2ObjectMap<ExtraFaceData> itemLayerFaceData);
    }
}
