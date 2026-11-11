package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IItemExtension#isPiglinCurrency} (Mekanism's refined glowstone ingot), which NeoForge's patches of {@link PiglinAi} call in place of
 * vanilla's {@code isBarterCurrency}.
 */
@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {

    @ModifyReturnValue(method = "isBarterCurrency", at = @At("RETURN"))
    private static boolean mekanism$isPiglinCurrency(boolean original, ItemStack stack) {
        return original || stack.getItem().isPiglinCurrency(stack);
    }
}
