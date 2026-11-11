package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Removes blocks broken by players through NeoForge's {@code IBlockExtension#onDestroyedByPlayer} (like NeoForge's patch of
 * {@link ServerPlayerGameMode#destroyBlock}), which lets blocks keep themselves until their drops have been handled (Mekanism's bounding blocks drop the
 * main block of a multi-block machine that way).
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeDestroyMixin {

    @Shadow
    @Final
    protected ServerPlayer player;

    @ModifyExpressionValue(method = "destroyBlock", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState mekanism$captureAdjustedState(BlockState adjustedState, @Share("adjustedState") LocalRef<BlockState> adjustedStateRef) {
        adjustedStateRef.set(adjustedState);
        return adjustedState;
    }

    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean mekanism$onDestroyedByPlayer(ServerLevel level, BlockPos pos, boolean isMoving, Operation<Boolean> original,
          @Share("adjustedState") LocalRef<BlockState> adjustedStateRef) {
        BlockState state = adjustedStateRef.get();
        if (state == null) {
            return original.call(level, pos, isMoving);
        }
        //Note: The tool hasn't been damaged yet at this point
        boolean willHarvest = !player.preventsBlockDrops() && player.hasCorrectToolForDrops(state);
        return state.onDestroyedByPlayer(level, pos, player, player.getMainHandItem().copy(), willHarvest, level.getFluidState(pos));
    }
}
