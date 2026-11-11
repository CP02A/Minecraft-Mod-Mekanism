package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mekanism.fabric.shim.ServerHooks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.tags.TagKey;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's gameplay enchantments ({@code IItemExtension#getEnchantmentLevel} and {@code IItemExtension#getAllEnchantments}, i.e. the enchantments
 * provided by Mekanism's modules and the electric bow's fire mode), like NeoForge's patches of {@link EnchantmentHelper}.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    private static final String GET_OR_DEFAULT = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;";

    @Inject(method = "getItemEnchantmentLevel", at = @At("HEAD"), cancellable = true)
    private static void mekanism$enchantmentLevel(Holder<Enchantment> enchantment, ItemInstance piece, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(piece.typeHolder().value().getEnchantmentLevel(piece, enchantment));
    }

    @WrapOperation(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V",
          at = @At(value = "INVOKE", target = GET_OR_DEFAULT))
    private static Object mekanism$iterationEnchantments(ItemStack stack, DataComponentType<?> type, Object fallback, Operation<Object> original,
          ItemStack piece, EnchantmentHelper.EnchantmentVisitor method) {
        HolderLookup.RegistryLookup<Enchantment> lookup = ServerHooks.enchantmentLookup();
        return lookup == null ? original.call(stack, type, fallback) : stack.getAllEnchantments(lookup);
    }

    @WrapOperation(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
          at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private static @Nullable Object mekanism$slotIterationEnchantments(ItemStack stack, DataComponentType<?> type, Operation<Object> original, ItemStack piece,
          EquipmentSlot slot, LivingEntity owner, EnchantmentHelper.EnchantmentInSlotVisitor method) {
        return stack.getAllEnchantments(owner.registryAccess().lookupOrThrow(Registries.ENCHANTMENT));
    }

    @WrapOperation(method = "hasTag", at = @At(value = "INVOKE", target = GET_OR_DEFAULT))
    private static Object mekanism$tagEnchantments(ItemStack stack, DataComponentType<?> type, Object fallback, Operation<Object> original, ItemStack item,
          TagKey<Enchantment> tag) {
        HolderLookup.RegistryLookup<Enchantment> lookup = ServerHooks.enchantmentLookup();
        return lookup == null ? original.call(stack, type, fallback) : stack.getAllEnchantments(lookup);
    }
}
