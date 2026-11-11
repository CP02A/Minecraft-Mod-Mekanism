package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IBlockExtension#onBlockExploded}: like NeoForge's patch of {@code BlockBehaviour#onExplosionHit}, the exploded block is removed by
 * {@code onBlockExploded} (whose default sets air and calls {@link Block#wasExploded}, like vanilla) instead of by vanilla's removal and
 * {@code wasExploded} call.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourExplosionMixin {

    @WrapOperation(method = "onExplosionHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean mekanism$onBlockExploded(ServerLevel level, BlockPos pos, BlockState air, Operation<Boolean> original, BlockState state, ServerLevel l,
          BlockPos p, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        state.getBlock().onBlockExploded(state, level, pos, explosion);
        return true;
    }

    @WrapOperation(method = "onExplosionHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;wasExploded(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;)V"))
    private void mekanism$skipWasExploded(Block block, ServerLevel level, BlockPos pos, Explosion explosion, Operation<Void> original) {
        //Already called by the default onBlockExploded
    }
}
