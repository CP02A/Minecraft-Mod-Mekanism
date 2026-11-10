/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.model.data.ModelData;

/**
 * Mekanism shim: The model data accessor of NeoForge's {@code IBlockGetterExtension}, injected into {@link BlockGetter}.
 */
public interface IBlockGetterExtension {

    /**
     * {@return the model data of the block entity at the given position}
     *
     * @apiNote Mekanism shim: Client levels and chunk meshing regions override this with NeoForge's {@code ModelDataManager} (snapshot), other
     * getters query the block entity directly.
     */
    default ModelData getModelData(BlockPos pos) {
        BlockEntity blockEntity = ((BlockGetter) this).getBlockEntity(pos);
        return blockEntity == null ? ModelData.EMPTY : blockEntity.getModelData();
    }
}
