package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's state, position and entity aware block friction ({@code IBlockStateExtension#getFriction}), used where NeoForge patches vanilla's
 * {@link Block#getFriction()} calls for the block below an entity (item entities and experience orbs, living entities are handled by
 * {@link LivingEntityMixin}).
 */
@Mixin({ItemEntity.class, ExperienceOrb.class})
public abstract class EntityFrictionMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private float mekanism$getFriction(Block block, Operation<Float> original) {
        Entity entity = (Entity) (Object) this;
        BlockPos pos = entity.getBlockPosBelowThatAffectsMyMovement();
        Level level = entity.level();
        return level.getBlockState(pos).getFriction(level, pos, entity);
    }
}
