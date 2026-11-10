package mekanism.fabric.mixin.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Type;
import mekanism.fabric.shim.client.ModelPropertiesBridge;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reads NeoForge's {@code transform}, {@code visibility} and item layer {@code neoforge_data} keys of json models.
 */
@Mixin(CuboidModel.Deserializer.class)
public abstract class CuboidModelDeserializerMixin {

    @Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/resources/model/cuboid/CuboidModel;",
          at = @At("RETURN"))
    private void mekanism$readNeoForgeProperties(JsonElement json, Type type, JsonDeserializationContext context, CallbackInfoReturnable<CuboidModel> cir) {
        JsonObject object = json.getAsJsonObject();
        ((ModelPropertiesBridge.CuboidModelProperties) (Object) cir.getReturnValue()).mekanism$setNeoForgeProperties(
              NeoForgeModelProperties.deserializeRootTransform(object, context),
              NeoForgeModelProperties.deserializePartVisibility(object),
              NeoForgeModelProperties.deserializeItemLayerFaceData(object)
        );
    }
}
