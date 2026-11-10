package mekanism.fabric.mixin.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.math.Transformation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.Map;
import mekanism.fabric.shim.client.ModelPropertiesBridge;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import net.neoforged.neoforge.common.util.TransformationHelper;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's root transform, part visibility and item layer face data properties of json models (read by {@link CuboidModelDeserializerMixin}), and
 * NeoForge's {@link Transformation} deserializer in {@link CuboidModel#GSON}.
 */
@Mixin(CuboidModel.class)
public abstract class CuboidModelMixin implements ModelPropertiesBridge.CuboidModelProperties {

    @Unique
    @Nullable
    private Transformation mekanism$rootTransform;
    @Unique
    private Map<String, Boolean> mekanism$partVisibility = Map.of();
    @Unique
    private Int2ObjectMap<ExtraFaceData> mekanism$itemLayerFaceData = Int2ObjectMaps.emptyMap();

    @WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/google/gson/GsonBuilder;create()Lcom/google/gson/Gson;"))
    private static Gson mekanism$addTransformationDeserializer(GsonBuilder builder, Operation<Gson> original) {
        return original.call(builder.registerTypeAdapter(Transformation.class, new TransformationHelper.Deserializer()));
    }

    @Override
    public void mekanism$setNeoForgeProperties(@Nullable Transformation rootTransform, Map<String, Boolean> partVisibility,
          Int2ObjectMap<ExtraFaceData> itemLayerFaceData) {
        this.mekanism$rootTransform = rootTransform;
        this.mekanism$partVisibility = partVisibility;
        this.mekanism$itemLayerFaceData = itemLayerFaceData;
    }

    /**
     * Overrides {@link net.neoforged.neoforge.client.extensions.UnbakedModelExtension#fillAdditionalProperties(ContextMap.Builder)}.
     */
    public void fillAdditionalProperties(ContextMap.Builder propertiesBuilder) {
        NeoForgeModelProperties.fillRootTransformProperty(propertiesBuilder, mekanism$rootTransform);
        //Note: The fields can be null for models that weren't created by the json deserializer (such as the missing model)
        if (mekanism$partVisibility != null) {
            NeoForgeModelProperties.fillPartVisibilityProperty(propertiesBuilder, mekanism$partVisibility);
        }
        if (mekanism$itemLayerFaceData != null) {
            NeoForgeModelProperties.fillItemLayerFaceData(propertiesBuilder, mekanism$itemLayerFaceData);
        }
    }
}
