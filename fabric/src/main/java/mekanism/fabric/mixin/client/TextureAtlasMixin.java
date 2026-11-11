package mekanism.fabric.mixin.client;

import net.minecraft.client.renderer.texture.TextureAtlas;
import mekanism.shim.neoforged.fml.ModLoader;
import mekanism.shim.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's {@link TextureAtlasStitchedEvent}.
 */
@Mixin(TextureAtlas.class)
public abstract class TextureAtlasMixin {

    @Inject(method = "upload", at = @At("TAIL"))
    private void mekanism$stitched(CallbackInfo ci) {
        ModLoader.postEvent(new TextureAtlasStitchedEvent((TextureAtlas) (Object) this));
    }
}
