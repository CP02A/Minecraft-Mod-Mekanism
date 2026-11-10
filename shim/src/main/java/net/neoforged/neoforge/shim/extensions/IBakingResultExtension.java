/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.client.resources.model.ModelBakery;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader;
import net.neoforged.neoforge.shim.ShimServices;

/**
 * Mekanism shim: The standalone models NeoForge adds to {@link ModelBakery.BakingResult}, injected as an interface.
 */
public interface IBakingResultExtension {

    default StandaloneModelLoader.BakedModels standaloneModels() {
        return ShimServices.CLIENT.getStandaloneModels();
    }
}
