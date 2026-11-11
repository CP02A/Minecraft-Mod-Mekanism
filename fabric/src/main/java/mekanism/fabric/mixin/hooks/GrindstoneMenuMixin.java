package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IItemExtension#isDamageable} (through {@code isCombineRepairable}, which has no other condition on Fabric) when the grindstone merges
 * two items: items that aren't damageable are merged like non damageable items, and an additional item that isn't damageable doesn't repair the input.
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {

    @ModifyExpressionValue(method = "mergeItems", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/item/ItemStack;isDamageableItem()Z"))
    private boolean mekanism$isCombineRepairable(boolean damageable, @Local(argsOnly = true, ordinal = 0) ItemStack input) {
        return damageable && input.getItem().isDamageable(input);
    }

    @WrapOperation(method = "mergeItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setDamageValue(I)V"))
    private void mekanism$keepDamage(ItemStack newItem, int damage, Operation<Void> original, @Local(argsOnly = true, ordinal = 0) ItemStack input,
          @Local(argsOnly = true, ordinal = 1) ItemStack additional) {
        original.call(newItem, additional.getItem().isDamageable(additional) ? damage : input.getDamageValue());
    }
}
