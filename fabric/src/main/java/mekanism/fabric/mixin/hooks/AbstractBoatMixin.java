package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's state, position and entity aware block friction ({@code IBlockStateExtension#getFriction}) for the blocks below a boat.
 */
@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin {

    @WrapOperation(method = "getGroundFriction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getFriction()F"))
    private float mekanism$getFriction(Block block, Operation<Float> original, @Local BlockState state, @Local BlockPos.MutableBlockPos pos) {
        AbstractBoat boat = (AbstractBoat) (Object) this;
        return state.getFriction(boat.level(), pos, boat);
    }
}
