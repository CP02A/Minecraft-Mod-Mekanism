package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Client side counterpart of {@link mekanism.fabric.mixin.hooks.ServerPlayerGameModeDestroyMixin} (like NeoForge's patch of
 * {@link MultiPlayerGameMode#destroyBlock}).
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeDestroyMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean mekanism$onDestroyedByPlayer(Level level, BlockPos pos, BlockState newState, int flags, Operation<Boolean> original) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return original.call(level, pos, newState, flags);
        }
        return level.getBlockState(pos).onDestroyedByPlayer(level, pos, player, player.getMainHandItem().copy(), false, level.getFluidState(pos));
    }
}
