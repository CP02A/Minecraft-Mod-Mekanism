package mekanism.fabric.mixin.client;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
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
