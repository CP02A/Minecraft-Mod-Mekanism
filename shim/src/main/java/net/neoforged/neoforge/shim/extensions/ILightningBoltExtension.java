/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.world.entity.LightningBolt;
import net.neoforged.neoforge.shim.ShimHooks;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link LightningBolt}, injected as an interface.
 */
public interface ILightningBoltExtension {

    /**
     * Sets the damage this lightning bolt deals.
     */
    default void setDamage(float damage) {
        ShimHooks.setLightningDamage((LightningBolt) this, damage);
    }
}
