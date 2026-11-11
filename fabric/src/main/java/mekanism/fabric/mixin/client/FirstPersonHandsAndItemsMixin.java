package mekanism.fabric.mixin.client;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's {@code IItemExtension#shouldCauseReequipAnimation} when the held item changed to a different item (Fabric API only asks
 * {@code FabricItem#allowComponentsUpdateAnimation} when the item stayed the same, which the shim bridges in {@code ItemMixin}): if the old item doesn't
 * want the animation, the new stack is shown right away. Mirrors NeoForge's {@code ClientHooks#shouldCauseReequipAnimation}, including the main hand slot
 * tracking.
 */
@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {

    @Shadow
    private ItemStack mainHandItem;
    @Shadow
    private ItemStack offHandItem;
    @Unique
    private int mekanism$slotMainHand;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"))
    private void mekanism$reequipAnimation(LocalPlayer player, CallbackInfo ci) {
        if (player.isHandsBusy()) {
            return;
        }
        int slot = player.getInventory().getSelectedSlot();
        boolean slotChanged = slot != mekanism$slotMainHand;
        mekanism$slotMainHand = slot;
        ItemStack nextMainHand = player.getMainHandItem();
        if (mekanism$suppressItemChange(mainHandItem, nextMainHand, slotChanged)) {
            mainHandItem = nextMainHand;
        }
        ItemStack nextOffHand = player.getOffhandItem();
        if (mekanism$suppressItemChange(offHandItem, nextOffHand, false)) {
            offHandItem = nextOffHand;
        }
    }

    @Unique
    private static boolean mekanism$suppressItemChange(ItemStack from, ItemStack to, boolean slotChanged) {
        //Same item: handled by Fabric API; empty stacks always animate
        return from != to && !from.isEmpty() && !to.isEmpty() && !to.is(from.getItem()) && !from.getItem().shouldCauseReequipAnimation(from, to, slotChanged);
    }
}
