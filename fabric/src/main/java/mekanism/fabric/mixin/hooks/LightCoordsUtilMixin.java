package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's level aware light emission ({@code IBlockStateExtension#getLightEmission}) for the light a block is rendered with, like NeoForge's patch
 * of {@link LightCoordsUtil#getLightCoords(LightCoordsUtil.BrightnessGetter, BlockAndLightGetter, BlockState, BlockPos)}.
 */
@Mixin(LightCoordsUtil.class)
public abstract class LightCoordsUtilMixin {

    @WrapOperation(method = "getLightCoords(Lnet/minecraft/util/LightCoordsUtil$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
          at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I"))
    private static int mekanism$emission(BlockState state, Operation<Integer> original, LightCoordsUtil.BrightnessGetter brightnessGetter, BlockAndLightGetter level,
          BlockState s, BlockPos pos) {
        return state.getLightEmission(level, pos);
    }
}
