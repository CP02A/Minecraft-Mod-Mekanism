package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IItemExtension#supportsEnchantment} in the enchant randomly loot function, like NeoForge's patch of {@link EnchantRandomlyFunction#run}.
 */
@Mixin(EnchantRandomlyFunction.class)
public abstract class EnchantRandomlyFunctionMixin {

    @WrapOperation(method = "lambda$run$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean mekanism$supportsEnchantment(Enchantment enchantment, ItemStack stack, Operation<Boolean> original,
          @Local(argsOnly = true) Holder<Enchantment> holder) {
        return stack.getItem().supportsEnchantment(stack, holder);
    }
}
