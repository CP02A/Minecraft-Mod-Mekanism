/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.fml.util.thread;

import mekanism.shim.neoforged.fml.LogicalSide;
import mekanism.shim.neoforged.neoforge.shim.ShimServices;

/**
 * Mekanism shim: Determines the logical side from the current thread.
 */
public class EffectiveSide {

    public static LogicalSide get() {
        return ShimServices.BACKEND.isOnServerThread() ? LogicalSide.SERVER : LogicalSide.CLIENT;
    }
}
