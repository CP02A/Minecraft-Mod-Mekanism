/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.world.level.biome.Biome;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link Biome}, injected as an interface.
 */
public interface IBiomeExtension {

    /**
     * {@return the climate settings of this biome}
     *
     * @apiNote Mekanism shim: Biome modifiers cannot change climate settings, so these are the biome's original settings.
     */
    default Biome.ClimateSettings getModifiedClimateSettings() {
        return ((Biome) (Object) this).climateSettings;
    }
}
