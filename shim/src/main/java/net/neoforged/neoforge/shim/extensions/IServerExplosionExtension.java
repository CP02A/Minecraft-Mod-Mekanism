/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link ServerExplosion}, injected as an interface.
 */
public interface IServerExplosionExtension {

    /**
     * Hurts the entities affected by the explosion.
     *
     * @param blocks The blocks affected by the explosion. Mekanism shim: Only used by NeoForge's explosion events, which are not fired.
     */
    default void hurtEntities(List<BlockPos> blocks) {
        ((ServerExplosion) this).hurtEntities();
    }
}
