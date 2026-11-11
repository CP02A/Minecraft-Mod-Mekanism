package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IBlockExtension#onCaughtFire} (see {@link TntBlockMixin}) like NeoForge's patch of {@code FireBlock#checkBurnOut}: the burnt block is
 * ignited through {@code onCaughtFire} (vanilla only primes TNT; NeoForge calls it for any burnt block before it's replaced, only TNT reacts to it).
 * Vanilla's method has no face, so none is passed.
 */
@Mixin(FireBlock.class)
public abstract class FireBlockIgnitionMixin {

    @WrapOperation(method = "checkBurnOut", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TntBlock;prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean mekanism$burntCaughtFire(Level level, BlockPos pos, Operation<Boolean> original, @Local BlockState oldState) {
        return oldState.getBlock().onCaughtFire(oldState, level, pos, null, null, ItemStack.EMPTY);
    }
}
