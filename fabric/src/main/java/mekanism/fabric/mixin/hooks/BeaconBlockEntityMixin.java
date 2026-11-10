package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's beacon beam colors ({@code IBlockStateExtension#getBeaconColorMultiplier}) instead of only {@link BeaconBeamBlock}s coloring the beam.
 * <p>
 * Vanilla only colors the beam for blocks that are {@link BeaconBeamBlock}s, using {@code BeaconBeamBlock#getColor().getTextureDiffuseColor()}. The block
 * checked by vanilla is replaced with a vanilla beacon beam block when the state has a color multiplier (or with air when it doesn't), and the dye color's
 * diffuse color with the state's color multiplier.
 */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;", ordinal = 0))
    private static Block mekanism$beamBlock(BlockState state, Operation<Block> original, Level level, BlockPos beaconPos,
          @Local(ordinal = 1) BlockPos checkPos, @Share("color") LocalRef<Integer> color) {
        Block block = original.call(state);
        Integer multiplier = state.getBeaconColorMultiplier(level, checkPos, beaconPos);
        color.set(multiplier);
        if (multiplier == null) {
            return block instanceof BeaconBeamBlock ? Blocks.AIR : block;
        }
        return block instanceof BeaconBeamBlock ? block : Blocks.STAINED_GLASS.white();
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/DyeColor;getTextureDiffuseColor()I"))
    private static int mekanism$beamColor(int original, @Share("color") LocalRef<Integer> color) {
        Integer multiplier = color.get();
        return multiplier == null ? original : multiplier;
    }
}
