/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.fml.event.lifecycle;

import mekanism.shim.neoforged.fml.ModContainer;

/**
 * Mekanism shim: Equivalent of FML's InterModProcessEvent, fired by the loader.
 */
public class InterModProcessEvent extends ParallelDispatchEvent {

    public InterModProcessEvent(ModContainer container) {
        super(container);
    }
}
