/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Mekanism shim: Base class of FML's lifecycle events.
 */
public abstract class ModLifecycleEvent extends Event implements IModBusEvent {

    private final ModContainer container;

    public ModLifecycleEvent(ModContainer container) {
        this.container = container;
    }

    public final String description() {
        return getClass().getSimpleName();
    }

    public ModContainer getContainer() {
        return container;
    }
}
