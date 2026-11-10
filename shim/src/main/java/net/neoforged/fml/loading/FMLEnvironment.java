/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.shim.ShimServices;

/**
 * Mekanism shim: Subset of FML's environment information, provided by the loader.
 */
public class FMLEnvironment {

    /**
     * {@return the physical distribution the game is running on}
     */
    public static Dist getDist() {
        return ShimServices.BACKEND.getDist();
    }

    /**
     * {@return {@code true} if the game is running in production, {@code false} in a development environment}
     */
    public static boolean isProduction() {
        return ShimServices.BACKEND.isProduction();
    }
}
