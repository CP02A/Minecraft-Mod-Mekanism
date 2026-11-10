/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link TerrainParticle}, injected as an interface.
 */
public interface ITerrainParticleExtension {

    /**
     * Updates the sprite of the particle to the particle sprite of the given state.
     *
     * @apiNote Mekanism shim: Position dependent particle sprites (NeoForge model data) are not supported.
     */
    default TerrainParticle updateSprite(BlockState state, @Nullable BlockPos pos) {
        TerrainParticle self = (TerrainParticle) this;
        self.setSprite(Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite());
        return self;
    }
}
