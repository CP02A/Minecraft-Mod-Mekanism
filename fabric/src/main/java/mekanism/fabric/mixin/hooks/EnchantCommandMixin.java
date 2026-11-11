package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IItemExtension#supportsEnchantment} in the enchant command, like NeoForge's patch of {@link EnchantCommand}.
 */
@Mixin(EnchantCommand.class)
public abstract class EnchantCommandMixin {

    @WrapOperation(method = "enchant", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean mekanism$supportsEnchantment(Enchantment enchantment, ItemStack stack, Operation<Boolean> original,
          @Local(argsOnly = true) Holder<Enchantment> holder) {
        return stack.getItem().supportsEnchantment(stack, holder);
    }
}
