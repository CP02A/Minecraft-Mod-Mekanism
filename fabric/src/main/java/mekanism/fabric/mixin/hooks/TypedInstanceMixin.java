package mekanism.fabric.mixin.hooks;

import net.minecraft.core.Holder;
import net.minecraft.core.TypedInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Like NeoForge's patch of {@link TypedInstance#is(Holder)}: compares the values of the holders instead of the holders themselves, so that custom holders
 * (such as {@link net.neoforged.neoforge.registries.DeferredHolder}s, which Mekanism's registry objects are) match the registered holder of the same
 * value. Vanilla compares the holders by identity, making checks like {@code state.is(MekanismBlocks.TELEPORTER_FRAME)} always fail.
 */
@Mixin(TypedInstance.class)
public interface TypedInstanceMixin<T> {

    @Shadow
    Holder<T> typeHolder();

    @Inject(method = "is(Lnet/minecraft/core/Holder;)Z", at = @At("HEAD"), cancellable = true)
    private void mekanism$isHolder(Holder<T> type, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(typeHolder().value() == type.value());
    }
}
