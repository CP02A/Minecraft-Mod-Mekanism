package mekanism.fabric.mixin.hooks;

import mekanism.fabric.shim.ServerHooks;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;

/**
 * NeoForge's gameplay enchantments ({@code IItemExtension#getAllEnchantments}) for enchantment item predicates (i.e. silk touch loot conditions), like
 * NeoForge's patch of {@link EnchantmentsPredicate.Enchantments}, which overrides {@link SingleComponentItemPredicate#matches(DataComponentGetter)}.
 */
@Mixin(EnchantmentsPredicate.Enchantments.class)
public abstract class EnchantmentsPredicateMixin implements SingleComponentItemPredicate<ItemEnchantments> {

    @Override
    public boolean matches(DataComponentGetter getter) {
        if (getter instanceof ItemStack stack) {
            HolderLookup.RegistryLookup<Enchantment> lookup = ServerHooks.enchantmentLookup();
            if (lookup != null) {
                return matches(stack.getAllEnchantments(lookup));
            }
        }
        //Vanilla's default
        ItemEnchantments value = getter.get(componentType());
        return value != null && matches(value);
    }
}
