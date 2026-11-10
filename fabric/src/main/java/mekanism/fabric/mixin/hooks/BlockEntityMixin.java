package mekanism.fabric.mixin.hooks;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge invalidates the capabilities at a block entity's position (notifying {@code BlockCapabilityCache}s such as those of transmitters) and
 * requests a model data update when the block entity is removed or placed.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {

    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void mekanism$onRemoved(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        self.invalidateCapabilities();
        self.requestModelDataUpdate();
    }

    @Inject(method = "clearRemoved", at = @At("TAIL"))
    private void mekanism$onPlaced(CallbackInfo ci) {
        ((BlockEntity) (Object) this).invalidateCapabilities();
    }
}
