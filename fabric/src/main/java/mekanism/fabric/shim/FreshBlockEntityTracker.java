package mekanism.fabric.shim;

import java.util.Collection;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Implemented by levels: queues block entities that were added to the level, so that NeoForge's {@code onLoad} hook is called for them on the next block
 * entity tick, like NeoForge does.
 */
public interface FreshBlockEntityTracker {

    void mekanism$addFreshBlockEntities(Collection<BlockEntity> blockEntities);
}
