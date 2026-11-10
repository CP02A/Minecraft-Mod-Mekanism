package mekanism.fabric.mixin.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import mekanism.fabric.shim.client.ModelDataSnapshotHolder;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * NeoForge's model data snapshot of chunk meshing regions (set by {@link RenderRegionCacheMixin}).
 */
@Mixin(RenderSectionRegion.class)
public abstract class RenderSectionRegionMixin implements ModelDataSnapshotHolder {

    @Unique
    private Long2ObjectFunction<ModelData> mekanism$modelDataSnapshot = ModelDataManager.EMPTY_SNAPSHOT;

    @Override
    public void mekanism$setModelDataSnapshot(Long2ObjectFunction<ModelData> snapshot) {
        this.mekanism$modelDataSnapshot = snapshot;
    }

    /**
     * Overrides {@code IBlockGetterExtension#getModelData(BlockPos)}.
     */
    public ModelData getModelData(BlockPos pos) {
        return mekanism$modelDataSnapshot.get(pos.asLong());
    }
}
