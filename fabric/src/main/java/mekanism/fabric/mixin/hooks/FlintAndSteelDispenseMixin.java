package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.FlintAndSteelDispenseItemBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IBlockExtension#onCaughtFire} (see {@link TntBlockMixin}) like NeoForge's patch of {@code FlintAndSteelDispenseItemBehavior#execute}:
 * TNT in front of the dispenser is ignited through {@code onCaughtFire}.
 */
@Mixin(FlintAndSteelDispenseItemBehavior.class)
public abstract class FlintAndSteelDispenseMixin {

    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/TntBlock;prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean mekanism$dispensedCaughtFire(Level level, BlockPos pos, @Nullable LivingEntity igniter, ItemStack item, Operation<Boolean> original,
          BlockSource source, ItemStack dispensed) {
        BlockState target = level.getBlockState(pos);
        Direction face = source.state().getValue(DispenserBlock.FACING).getOpposite();
        return target.getBlock().onCaughtFire(target, level, pos, face, igniter, item);
    }
}
