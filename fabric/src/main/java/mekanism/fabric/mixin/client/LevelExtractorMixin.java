package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mekanism.fabric.shim.client.BlockOutlineBridge;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's block outline extraction event. The custom outline renderers are tracked by {@link BlockOutlineBridge}.
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    @Shadow
    private ClientLevel level;

    @Inject(method = "extractBlockOutline", at = @At("HEAD"))
    private void mekanism$startOutline(Camera camera, LevelRenderState levelRenderState, CallbackInfo ci) {
        BlockOutlineBridge.startFrame();
    }

    @Inject(method = "extractBlockOutline", cancellable = true, at = @At(value = "INVOKE", ordinal = 0,
          target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private void mekanism$extractOutline(Camera camera, LevelRenderState levelRenderState, CallbackInfo ci, @Local BlockHitResult hitResult, @Local BlockPos pos,
          @Local BlockState state, @Local(ordinal = 0) boolean isBlockTranslucent, @Local(ordinal = 1) boolean highContrast, @Local CollisionContext context) {
        ExtractBlockOutlineRenderStateEvent event = new ExtractBlockOutlineRenderStateEvent((LevelExtractor) (Object) this, level, pos, state, hitResult, context,
              isBlockTranslucent, highContrast, camera, levelRenderState);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            ci.cancel();
        } else {
            BlockOutlineBridge.setPending(event.getCustomRenderers());
        }
    }

    @Inject(method = "extractBlockOutline", at = @At("TAIL"))
    private void mekanism$attachRenderers(Camera camera, LevelRenderState levelRenderState, CallbackInfo ci) {
        BlockOutlineBridge.attachPending(levelRenderState.blockOutlineRenderState);
    }
}
