package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's level aware light emission ({@code IBlockStateExtension#getLightEmission}) for block light propagation, like NeoForge's patch of
 * {@link BlockLightEngine}.
 */
@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin {

    @Shadow
    @Final
    private BlockPos.MutableBlockPos mutablePos;

    @Unique
    private BlockGetter mekanism$level() {
        return ((LightEngineAccessor) (Object) this).mekanism$getChunkSource().getLevel();
    }

    @WrapOperation(method = "getEmission", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I"))
    private int mekanism$emission(BlockState state, Operation<Integer> original) {
        //Note: Like on NeoForge, the caller (checkNode) set mutablePos to the position of the block
        return state.getLightEmission(mekanism$level(), mutablePos);
    }

    @WrapOperation(method = "lambda$propagateLightSources$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I"))
    private int mekanism$sourceEmission(BlockState state, Operation<Integer> original, BlockPos lightPos, BlockState s) {
        return state.getLightEmission(mekanism$level(), lightPos);
    }
}
