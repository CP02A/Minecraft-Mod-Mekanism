/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import net.neoforged.fml.ModContainer;

/**
 * Mekanism shim: Equivalent of FML's FMLLoadCompleteEvent, fired by the loader.
 */
public class FMLLoadCompleteEvent extends ParallelDispatchEvent {

    public FMLLoadCompleteEvent(ModContainer container) {
        super(container);
    }
}
