package mekanism.fabric.mixin.client;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mekanism.fabric.shim.client.BlockStateModelBridge;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rewrites NeoForge's custom block state model types to Fabric's format when loading blockstate files.
 */
@Mixin(BlockStateModelLoader.class)
public abstract class BlockStateModelLoaderMixin {

    @ModifyExpressionValue(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StrictJsonParser;parse(Ljava/io/Reader;)Lcom/google/gson/JsonElement;"),
          require = 1)
    private static JsonElement mekanism$rewriteNeoForgeModels(JsonElement json) {
        return BlockStateModelBridge.rewrite(json);
    }
}
