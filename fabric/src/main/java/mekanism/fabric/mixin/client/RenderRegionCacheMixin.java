package mekanism.fabric.mixin.client;

import mekanism.fabric.shim.client.ModelDataSnapshotHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Like NeoForge, snapshots the model data of a region's sections on the main thread when the region is created, so that chunk meshing (on worker
 * threads) doesn't read it from the block entities.
 */
@Mixin(RenderRegionCache.class)
public abstract class RenderRegionCacheMixin {

    @Inject(method = "createRegion", at = @At("RETURN"))
    private void mekanism$snapshotModelData(ClientLevel level, long sectionNode, CallbackInfoReturnable<RenderSectionRegion> cir) {
        int sectionX = SectionPos.x(sectionNode);
        int sectionY = SectionPos.y(sectionNode);
        int sectionZ = SectionPos.z(sectionNode);
        ((ModelDataSnapshotHolder) cir.getReturnValue()).mekanism$setModelDataSnapshot(level.getModelDataManager().snapshotSectionRegion(
              sectionX - 1, sectionY - 1, sectionZ - 1, sectionX + 1, sectionY + 1, sectionZ + 1));
    }
}
