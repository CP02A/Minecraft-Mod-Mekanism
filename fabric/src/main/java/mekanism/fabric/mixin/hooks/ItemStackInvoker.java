package mekanism.fabric.mixin.hooks;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemStack.class)
public interface ItemStackInvoker {

    @Invoker("applyAfterUseComponentSideEffects")
    ItemStack mekanism$applyAfterUseComponentSideEffects(LivingEntity user, ItemStack stackBeforeUse);
}
