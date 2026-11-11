package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IBlockExtension#shouldDisplayFluidOverlay} (Mekanism's structural glass and the laser focus matrix show the fluid overlay texture like
 * glass), like NeoForge's patch of {@link FluidRenderer#tesselate}, which uses it in place of vanilla's {@code HalfTransparentBlock}/{@code LeavesBlock}
 * check (its default). Fabric API replaces the {@code HalfTransparentBlock} check with its transparent block registry, so the hook is added to the
 * {@code LeavesBlock} check, which is only reached when that one is false.
 */
@Mixin(FluidRenderer.class)
public abstract class FluidRendererOverlayMixin {

    @ModifyExpressionValue(method = "tesselate", at = @At(value = "CONSTANT", args = "classValue=net/minecraft/world/level/block/LeavesBlock"))
    private boolean mekanism$shouldDisplayFluidOverlay(boolean isLeaves, @Local(argsOnly = true) BlockAndTintGetter level, @Local(argsOnly = true) BlockPos pos,
          @Local(argsOnly = true) FluidState fluidState, @Local(name = "faceState") BlockState faceState, @Local(name = "faceDir") Direction faceDir) {
        return isLeaves || faceState.getBlock().shouldDisplayFluidOverlay(faceState, level, pos.relative(faceDir), fluidState);
    }
}
