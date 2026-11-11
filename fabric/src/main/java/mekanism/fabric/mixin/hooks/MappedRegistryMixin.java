package mekanism.fabric.mixin.hooks;

import mekanism.fabric.shim.FabricShimBackend;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Calls the bake callbacks of NeoForge datapack registries when they get frozen (like NeoForge's patch of {@link MappedRegistry#freeze()}).
 */
@Mixin(MappedRegistry.class)
public class MappedRegistryMixin {

    @Inject(method = "freeze", at = @At("RETURN"))
    private void mekanism$onFreeze(CallbackInfoReturnable<Registry<?>> cir) {
        FabricShimBackend.onRegistryFrozen((Registry<?>) this);
    }
}
