package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fires NeoForge's {@link ItemAttributeModifierEvent} when the attribute modifiers of a stack are iterated.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @ModifyExpressionValue(method = "forEachModifier", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object mekanism$modifyAttributes(Object modifiers) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.isEmpty() || !(modifiers instanceof ItemAttributeModifiers attributeModifiers)) {
            return modifiers;
        }
        return NeoForge.EVENT_BUS.post(new ItemAttributeModifierEvent(stack, attributeModifiers)).build();
    }
}
