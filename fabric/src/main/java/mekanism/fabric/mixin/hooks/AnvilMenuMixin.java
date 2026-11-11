package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IItemExtension#supportsEnchantment} in the anvil, like NeoForge's patch of {@link AnvilMenu#createResult} (its default also accepts
 * enchanted books, like vanilla's extra check after this call; creative players can still combine anything).
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @WrapOperation(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean mekanism$supportsEnchantment(Enchantment enchantment, ItemStack input, Operation<Boolean> original,
          @Local(name = "enchantmentHolder") Holder<Enchantment> holder) {
        return input.getItem().supportsEnchantment(input, holder);
    }
}
