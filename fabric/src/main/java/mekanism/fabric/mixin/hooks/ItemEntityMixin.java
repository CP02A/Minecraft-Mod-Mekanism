package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's damage source aware {@code IItemExtension#onDestroyed} (used by Mekanism to drop the contents of destroyed items), like NeoForge's patch of
 * {@link ItemEntity#hurtServer}. The default calls vanilla's {@code Item#onDestroyed}.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;onDestroyed(Lnet/minecraft/world/entity/item/ItemEntity;)V"))
    private void mekanism$onDestroyed(ItemStack stack, ItemEntity itemEntity, Operation<Void> original, ServerLevel level, DamageSource source, float damage) {
        stack.getItem().onDestroyed(itemEntity, source);
    }
}
