package mekanism.fabric.mixin.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Only validates that holder sets are non-empty when they can be resolved immediately (so not for NeoForge's custom holder sets that contain tags), like
 * NeoForge does.
 */
@Mixin(ExtraCodecs.class)
public abstract class ExtraCodecsMixin {

    @Inject(method = "nonEmptyHolderSet", at = @At("HEAD"), cancellable = true)
    private static <T> void mekanism$validateResolvableOnly(Codec<HolderSet<T>> listCodec, CallbackInfoReturnable<Codec<HolderSet<T>>> cir) {
        cir.setReturnValue(listCodec.validate(list -> list.isImmediatelyResolvable() && list.unwrap().right().filter(List::isEmpty).isPresent()
                                                      ? DataResult.error(() -> "List must have contents") : DataResult.success(list)));
    }
}
