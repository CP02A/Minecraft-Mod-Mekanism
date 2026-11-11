package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IBlockEntityExtension#handleUpdateTag}: the update tags of the block entities in a chunk data packet are handled by the block entity
 * (Mekanism reads its reduced update tag there) instead of being loaded like saved data, like NeoForge's patch of {@code LevelChunk#replaceWithPacketData}.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkUpdateTagMixin {

    @WrapOperation(method = "lambda$replaceWithPacketData$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private void mekanism$handleUpdateTag(BlockEntity blockEntity, ValueInput input, Operation<Void> original) {
        blockEntity.handleUpdateTag(input);
    }
}
