package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's level aware light emission ({@code IBlockStateExtension#getLightEmission}) when checking if a block change needs a light update, like
 * NeoForge's patch of {@code LevelChunk#setBlockState} (which calls its position aware {@code LightEngine#hasDifferentLightProperties}).
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkLightMixin {

    @WrapOperation(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LightEngine;hasDifferentLightProperties(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean mekanism$differentLight(BlockState oldState, BlockState newState, Operation<Boolean> original, BlockPos pos, BlockState state, int flags) {
        return original.call(oldState, newState) ||
               newState != oldState && newState.getLightEmission((LevelChunk) (Object) this, pos) != oldState.getLightEmission((LevelChunk) (Object) this, pos);
    }
}
