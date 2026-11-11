package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IItemExtension#canWalkOnPowderedSnow} (Mekanism's free runners and MekaSuit boots) in place of vanilla's leather boots check, like
 * NeoForge's patch of {@link PowderSnowBlock#canEntityWalkOnPowderSnow}.
 */
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {

    @WrapOperation(method = "canEntityWalkOnPowderSnow", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
    private static boolean mekanism$canWalkOnPowderedSnow(ItemStack stack, Object item, Operation<Boolean> original, Entity entity) {
        return stack.getItem().canWalkOnPowderedSnow(stack, (LivingEntity) entity);
    }
}
