package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's gliding with items that provide the {@link NeoForgeMod#GLIDING_FLIGHT gliding flight attribute} (the MekaSuit's elytra unit adds it with
 * {@code ItemAttributeModifierEvent}):
 * <ul>
 *     <li>like NeoForge's patch of {@link LivingEntity#canGlideUsing}, an item whose attribute modifiers for the slot give gliding flight can be glided with
 *     (vanilla only checks the glider component), so {@code canGlide} and the glide damage slot selection find it</li>
 *     <li>{@code IItemExtension#onGlideDamage} (the elytra unit uses energy instead of durability) in place of vanilla's durability damage, like
 *     NeoForge's patch of {@code LivingEntity#updateFallFlying}. The default damages the item like vanilla.</li>
 * </ul>
 * NeoForge's server side {@code canGlide} only checks the entity's gliding flight attribute (so directly applied modifiers, like the elytra unit's
 * disengage modifier, apply); this keeps vanilla's per item check.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGlideMixin {

    @Inject(method = "canGlideUsing", at = @At("HEAD"), cancellable = true)
    private static void mekanism$glidingFlightAttribute(ItemStack stack, EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
        if (!stack.isEmpty()) {
            //Base 0, like NeoForge's ItemAttributeModifiers#compute(attribute, 0, slot)
            double[] value = {0, 0, 1};
            stack.forEachModifier(slot, (attribute, modifier) -> {
                //Note: Compare the values, NeoForge's holder is a deferred holder
                if (attribute.value() == NeoForgeMod.GLIDING_FLIGHT.value()) {
                    switch (modifier.operation()) {
                        case AttributeModifier.Operation.ADD_VALUE -> value[0] += modifier.amount();
                        case AttributeModifier.Operation.ADD_MULTIPLIED_BASE -> value[1] += modifier.amount();
                        case AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL -> value[2] *= 1 + modifier.amount();
                    }
                }
            });
            if ((value[0] + value[0] * value[1]) * value[2] > 0) {
                cir.setReturnValue(true);
            }
        }
    }

    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void mekanism$onGlideDamage(ItemStack stack, int amount, LivingEntity wearer, EquipmentSlot slot, Operation<Void> original) {
        stack.getItem().onGlideDamage(stack, wearer, slot);
    }
}
