package mekanism.fabric.mixin.hooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import mekanism.fabric.shim.FreshBlockEntityTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Calls NeoForge's {@code BlockEntity#onLoad} for block entities that were added to the level, at the start of the next block entity tick.
 */
@Mixin(Level.class)
public abstract class LevelMixin implements FreshBlockEntityTracker {

    @Unique
    private final List<BlockEntity> mekanism$freshBlockEntities = new ArrayList<>();
    @Unique
    private final List<BlockEntity> mekanism$pendingFreshBlockEntities = new ArrayList<>();
    @Unique
    private boolean mekanism$loadingBlockEntities;

    @Override
    public void mekanism$addFreshBlockEntities(Collection<BlockEntity> blockEntities) {
        (mekanism$loadingBlockEntities ? mekanism$pendingFreshBlockEntities : mekanism$freshBlockEntities).addAll(blockEntities);
    }

    @Inject(method = "tickBlockEntities", at = @At("HEAD"))
    private void mekanism$loadFreshBlockEntities(CallbackInfo ci) {
        if (!mekanism$pendingFreshBlockEntities.isEmpty()) {
            mekanism$freshBlockEntities.addAll(mekanism$pendingFreshBlockEntities);
            mekanism$pendingFreshBlockEntities.clear();
        }
        if (!mekanism$freshBlockEntities.isEmpty()) {
            mekanism$loadingBlockEntities = true;
            try {
                for (BlockEntity blockEntity : mekanism$freshBlockEntities) {
                    //Only call onLoad on block entities that have been fully added to the level
                    if (!blockEntity.isRemoved() && blockEntity.hasLevel()) {
                        blockEntity.onLoad();
                    }
                }
            } finally {
                mekanism$loadingBlockEntities = false;
            }
            mekanism$freshBlockEntities.clear();
        }
    }
}
