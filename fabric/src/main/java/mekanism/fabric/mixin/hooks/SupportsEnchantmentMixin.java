package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IItemExtension#supportsEnchantment} (Mekanism's gear only takes enchantments while enchantable, the electric bow never takes flame, the
 * scuba mask takes aqua affinity) for the enchanting table, like NeoForge's patch of {@link EnchantmentHelper#getAvailableEnchantmentResults}, which
 * filters with {@code IItemExtension#isPrimaryItemFor} (whose default is vanilla's {@link Enchantment#isPrimaryItem} with {@code supportsEnchantment}
 * in place of {@link Enchantment#isSupportedItem}; Mekanism doesn't override it). See {@link AnvilMenuMixin} and {@link EnchantCommandMixin} for the
 * other call sites.
 */
@Mixin(EnchantmentHelper.class)
public abstract class SupportsEnchantmentMixin {

    @WrapOperation(method = "lambda$getAvailableEnchantmentResults$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;isPrimaryItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean mekanism$isPrimaryItemFor(Enchantment enchantment, ItemStack stack, Operation<Boolean> original, @Local(argsOnly = true) Holder<Enchantment> holder) {
        Optional<HolderSet<Item>> primaryItems = enchantment.definition().primaryItems();
        return stack.getItem().supportsEnchantment(stack, holder) && (primaryItems.isEmpty() || stack.is(primaryItems.get()));
    }
}
