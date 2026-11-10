/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.shim.ShimHooks;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's {@code ILevelExtension}, injected into {@link Level}.
 */
public interface ILevelExtension {

    String TRANSLATION_PREFIX = "dimension";

    private Level self() {
        return (Level) this;
    }

    default <T, C extends @Nullable Object> @Nullable T getCapability(BlockCapability<T, C> cap, BlockPos pos, C context) {
        return cap.getCapability(self(), pos, null, null, context);
    }

    default <T, C extends @Nullable Object> @Nullable T getCapability(BlockCapability<T, C> cap, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity,
          C context) {
        return cap.getCapability(self(), pos, state, blockEntity, context);
    }

    default <T> @Nullable T getCapability(BlockCapability<T, @Nullable Void> cap, BlockPos pos) {
        return cap.getCapability(self(), pos, null, null, null);
    }

    default <T> @Nullable T getCapability(BlockCapability<T, @Nullable Void> cap, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity) {
        return cap.getCapability(self(), pos, state, blockEntity, null);
    }

    /**
     * Notify all listeners that the capabilities at a specific position might have changed.
     */
    default void invalidateCapabilities(BlockPos pos) {
        ShimHooks.invalidateCapabilities(self(), pos);
    }

    /**
     * Notify all listeners that the capabilities at all the positions in a chunk might have changed.
     */
    default void invalidateCapabilities(ChunkPos pos) {
        ShimHooks.invalidateCapabilities(self(), pos);
    }

    /**
     * Retrieves the model data manager for this level, which is only present for client levels.
     *
     * @apiNote Mekanism shim: Overridden for client levels by the loader.
     */
    default net.neoforged.neoforge.model.data.@Nullable ModelDataManager getModelDataManager() {
        return null;
    }

    /**
     * {@return the translated description of this dimension, with a fallback to the registry name}
     */
    default Component getDescription() {
        var dimension = self().dimension().identifier();
        return Component.translatableWithFallback(dimension.toLanguageKey(TRANSLATION_PREFIX), dimension.toString());
    }
}
