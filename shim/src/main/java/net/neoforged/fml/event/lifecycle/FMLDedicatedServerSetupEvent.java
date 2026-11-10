/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import net.neoforged.fml.ModContainer;

/**
 * Mekanism shim: Equivalent of FML's FMLDedicatedServerSetupEvent, fired by the loader.
 */
public class FMLDedicatedServerSetupEvent extends ParallelDispatchEvent {

    public FMLDedicatedServerSetupEvent(ModContainer container) {
        super(container);
    }
}
