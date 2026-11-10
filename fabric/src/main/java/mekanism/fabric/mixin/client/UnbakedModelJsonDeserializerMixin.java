package mekanism.fabric.mixin.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import java.lang.reflect.Type;
import net.fabricmc.fabric.impl.client.model.loading.UnbakedModelJsonDeserializer;
import net.minecraft.client.resources.model.UnbakedModel;
import net.neoforged.neoforge.client.model.UnbakedModelParser;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds support for NeoForge's {@code loader} key (custom {@link net.neoforged.neoforge.client.model.UnbakedModelLoader}s) to Fabric's unbaked
 * model deserializer, which replaces vanilla's in {@code CuboidModel.GSON} and only knows Fabric's {@code fabric:type} key.
 */
@Mixin(value = UnbakedModelJsonDeserializer.class, remap = false)
public abstract class UnbakedModelJsonDeserializerMixin {

    @Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/resources/model/UnbakedModel;",
          at = @At("HEAD"), cancellable = true)
    private void mekanism$neoForgeLoader(JsonElement json, Type type, JsonDeserializationContext context, CallbackInfoReturnable<UnbakedModel> cir) {
        if (json.isJsonObject()) {
            UnbakedModel model = UnbakedModelParser.deserializeWithLoader(json.getAsJsonObject(), context);
            if (model != null) {
                cir.setReturnValue(model);
            }
        }
    }
}
