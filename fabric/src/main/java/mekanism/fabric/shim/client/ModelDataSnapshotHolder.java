package mekanism.fabric.shim.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * Added to {@code RenderSectionRegion}: holds the region's model data snapshot, taken on the main thread.
 */
public interface ModelDataSnapshotHolder {

    void mekanism$setModelDataSnapshot(Long2ObjectFunction<ModelData> snapshot);
}
