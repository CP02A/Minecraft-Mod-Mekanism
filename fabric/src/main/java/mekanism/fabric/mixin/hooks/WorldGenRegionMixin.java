package mekanism.fabric.mixin.hooks;

import mekanism.fabric.shim.ServerHooks;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Doesn't add mobs whose spawn was cancelled by NeoForge's {@code FinalizeSpawnEvent} during world generation, like NeoForge's patch.
 */
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void mekanism$spawnCancelled(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ServerHooks.isSpawnCancelled(entity)) {
            cir.setReturnValue(false);
        }
    }
}
