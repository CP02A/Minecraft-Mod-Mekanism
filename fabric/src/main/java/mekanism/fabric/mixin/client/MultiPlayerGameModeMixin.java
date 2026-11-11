package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mekanism.fabric.shim.ItemUseHooks;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client side counterpart of {@link mekanism.fabric.mixin.hooks.ServerPlayerGameModeMixin} (like NeoForge's patch of
 * {@code MultiPlayerGameMode#performUseItemOn}).
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "performUseItemOn", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSecondaryUseActive()Z"))
    private void mekanism$onItemUseFirst(LocalPlayer player, InteractionHand hand, BlockHitResult blockHit, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = ItemUseHooks.onItemUseFirst(player.getItemInHand(hand), new UseOnContext(player, hand, blockHit));
        if (result != InteractionResult.PASS) {
            cir.setReturnValue(result);
        }
    }

    @ModifyExpressionValue(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSecondaryUseActive()Z"))
    private boolean mekanism$doesSneakBypassUse(boolean secondaryUseActive, LocalPlayer player, InteractionHand hand, BlockHitResult blockHit) {
        return secondaryUseActive && !ItemUseHooks.sneakBypassesUse(player, player.level(), blockHit.getBlockPos());
    }
}
