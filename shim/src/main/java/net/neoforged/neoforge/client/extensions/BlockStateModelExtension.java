/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.client.extensions;

import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockStateExtension;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/// @implNote Mekanism shim: {@code createGeometryKey} and {@code particleMaterial} are provided by Fabric's own block state model extension,
/// which has the same signatures and semantics.
public interface BlockStateModelExtension {
    private BlockStateModel self() {
        return (BlockStateModel) this;
    }


    /**
     * Collects the parts of the model that should be rendered.
     *
     * <p>Typically called on a meshing worker thread, with a snapshot of world state.
     * To access block entity data, use {@code IBlockGetterExtension#getModelData(BlockPos)} on the passed level.
     * Avoid accessing or manipulating the block entities directly as they are not thread safe.
     * Other world state is safe to access.
     *
     * <p>The parameters passed to this method might not be what the model expects.
     * For example, the model data might be missing or coming from a different/outdated block entity,
     * the block state in the level might be different from the one passed in, and so on...
     * The model should handle these cases gracefully and return the best model it can
     * (e.g. by returning a completely unconnected model, in the case of connected textures).
     *
     * <p>Calling {@link IBlockStateExtension#getAppearance} before accessing the block state is recommended.
     *
     * @param level  a level to query block entity data or other world state
     * @param pos    the position of the block being rendered
     * @param state  the state of the block being rendered
     * @param random a random source for random model variations
     * @param parts  the list that should receive all parts to be rendered
     */
    default void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        self().collectParts(random, parts);
    }


    /// Returns the material flags of this model.
    ///
    /// @param level a level to query block entity data or other world state
    /// @param pos   the position of the block being rendered
    /// @param state the state of the block being rendered
    @BakedQuad.MaterialFlags
    default int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return self().materialFlags();
    }

    /// Returns whether this model has the provided material flag.
    ///
    /// @param level a level to query block entity data or other world state
    /// @param pos   the position of the block being rendered
    /// @param state the state of the block being rendered
    /// @param flag  the material flag to check
    @ApiStatus.NonExtendable
    default boolean hasMaterialFlag(BlockAndTintGetter level, BlockPos pos, BlockState state, @BakedQuad.MaterialFlags int flag) {
        return (self().materialFlags(level, pos, state) & flag) != 0;
    }
}
