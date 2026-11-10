/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.event;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.bus.api.Event;

/**
 * Fired to register server side reload listeners.
 *
 * <p>Mekanism shim: Fired once by the loader on the game bus during initialization; the listeners are then registered with the loader's resource reload system.
 * Dependency ordering between listeners is not supported.
 */
public class AddServerReloadListenersEvent extends Event {

    private final Map<Identifier, PreparableReloadListener> listeners = new LinkedHashMap<>();

    public AddServerReloadListenersEvent() {
    }

    public void addListener(Identifier key, PreparableReloadListener listener) {
        if (this.listeners.containsKey(key) || this.listeners.containsValue(listener)) {
            throw new IllegalArgumentException("Attempted to register two reload listeners for the same key: " + key);
        }
        this.listeners.put(key, listener);
    }

    /**
     * Mekanism shim: Dependencies between listeners are ignored.
     */
    public void addDependency(Identifier first, Identifier second) {
    }

    /**
     * @apiNote Mekanism shim: Used by the loader to register the listeners.
     */
    public Map<Identifier, PreparableReloadListener> getListeners() {
        return Collections.unmodifiableMap(this.listeners);
    }
}
