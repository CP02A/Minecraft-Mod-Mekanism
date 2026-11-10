package mekanism.fabric.mixin.hooks;

import java.util.List;
import mekanism.fabric.shim.FreshBlockEntityTracker;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's block entity load and chunk unload hooks.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Shadow
    @Final
    private Level level;

    @Shadow
    public abstract Map<BlockPos, BlockEntity> getBlockEntities();

    @Inject(method = "addAndRegisterBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;updateBlockEntityTicker(Lnet/minecraft/world/level/block/entity/BlockEntity;)V", shift = At.Shift.AFTER))
    private void mekanism$queueOnLoad(BlockEntity blockEntity, CallbackInfo ci) {
        ((FreshBlockEntityTracker) level).mekanism$addFreshBlockEntities(List.of(blockEntity));
    }

    @Inject(method = "registerAllBlockEntitiesAfterLevelLoad", at = @At("HEAD"))
    private void mekanism$queueOnLoadAll(CallbackInfo ci) {
        ((FreshBlockEntityTracker) level).mekanism$addFreshBlockEntities(getBlockEntities().values());
    }

    @Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
    private void mekanism$onChunkUnloaded(CallbackInfo ci) {
        getBlockEntities().values().forEach(BlockEntity::onChunkUnloaded);
    }
}
