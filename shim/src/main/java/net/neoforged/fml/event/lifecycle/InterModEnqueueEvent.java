/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import net.neoforged.fml.ModContainer;

/**
 * Mekanism shim: Equivalent of FML's InterModEnqueueEvent, fired by the loader.
 */
public class InterModEnqueueEvent extends ParallelDispatchEvent {

    public InterModEnqueueEvent(ModContainer container) {
        super(container);
    }
}
