package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's state, position and entity aware block friction ({@code IBlockStateExtension#getFriction}) for the angular friction of leashed entities
 * on the ground.
 */
@Mixin(Leashable.class)
public interface LeashableMixin {

    @WrapOperation(method = "angularFriction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private static float mekanism$getFriction(Block block, Operation<Float> original, @Local(argsOnly = true) Entity entity) {
        BlockPos pos = entity.getBlockPosBelowThatAffectsMyMovement();
        Level level = entity.level();
        return level.getBlockState(pos).getFriction(level, pos, entity);
    }
}
