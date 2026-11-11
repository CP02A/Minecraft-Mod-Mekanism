package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IItemExtension#onGlideDamage} (the MekaSuit's elytra unit uses energy instead of durability) in place of vanilla's durability damage,
 * like NeoForge's patch of {@code LivingEntity#updateFallFlying}. The default damages the item like vanilla.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGlideMixin {

    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void mekanism$onGlideDamage(ItemStack stack, int amount, LivingEntity wearer, EquipmentSlot slot, Operation<Void> original) {
        stack.getItem().onGlideDamage(stack, wearer, slot);
    }
}
