package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IItemExtension#isDamageable}, which NeoForge checks when combining two items in the crafting grid to repair them
 * ({@code Item#isCombineRepairable}).
 */
@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin {

    @ModifyReturnValue(method = "canCombine", at = @At("RETURN"))
    private static boolean mekanism$isDamageable(boolean canCombine, ItemStack first, ItemStack second) {
        return canCombine && first.getItem().isDamageable(first) && second.getItem().isDamageable(second);
    }
}
