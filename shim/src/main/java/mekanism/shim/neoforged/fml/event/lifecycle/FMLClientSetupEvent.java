/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.fml.event.lifecycle;

import mekanism.shim.neoforged.fml.ModContainer;

/**
 * Mekanism shim: Equivalent of FML's FMLClientSetupEvent, fired by the loader.
 */
public class FMLClientSetupEvent extends ParallelDispatchEvent {

    public FMLClientSetupEvent(ModContainer container) {
        super(container);
    }
}
