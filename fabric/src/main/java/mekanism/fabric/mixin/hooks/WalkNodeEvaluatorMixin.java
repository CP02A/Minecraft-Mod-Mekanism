package mekanism.fabric.mixin.hooks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's {@code IBlockExtension#getBlockPathType} (Mekanism's multiblock blocks are fences for mobs inside a formed multiblock), like NeoForge's patch of
 * {@link WalkNodeEvaluator#getPathTypeFromState}: a non-null path type of the block is used instead of vanilla's. NeoForge passes no mob.
 */
@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {

    @Inject(method = "getPathTypeFromState", at = @At("HEAD"), cancellable = true)
    private static void mekanism$blockPathType(BlockGetter level, BlockPos pos, CallbackInfoReturnable<PathType> cir) {
        BlockState state = level.getBlockState(pos);
        PathType type = state.getBlock().getBlockPathType(state, level, pos, null);
        if (type != null) {
            cir.setReturnValue(type);
        }
    }
}
