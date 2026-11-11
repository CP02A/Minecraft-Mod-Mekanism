package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import mekanism.fabric.shim.ItemUseHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Calls NeoForge's {@code IItemExtension#onItemUseFirst} before a block is used, and lets {@code IItemExtension#doesSneakBypassUse} allow using blocks
 * while sneaking (like NeoForge's patch of {@link ServerPlayerGameMode#useItemOn}).
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Inject(method = "useItemOn", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSecondaryUseActive()Z"))
    private void mekanism$onItemUseFirst(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand, BlockHitResult hitResult,
          CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = ItemUseHooks.onItemUseFirst(itemStack, new UseOnContext(player, hand, hitResult));
        if (result != InteractionResult.PASS) {
            cir.setReturnValue(result);
        }
    }

    @ModifyExpressionValue(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSecondaryUseActive()Z"))
    private boolean mekanism$doesSneakBypassUse(boolean secondaryUseActive, ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand,
          BlockHitResult hitResult) {
        return secondaryUseActive && !ItemUseHooks.sneakBypassesUse(player, level, hitResult.getBlockPos());
    }
}
