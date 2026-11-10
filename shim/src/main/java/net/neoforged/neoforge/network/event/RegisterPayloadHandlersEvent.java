/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.network.event;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Fired on the mod bus to register payloads and their handlers.
 *
 * <p>Mekanism shim: Fired by the loader during mod initialization.
 */
public class RegisterPayloadHandlersEvent extends Event implements IModBusEvent {

    public RegisterPayloadHandlersEvent() {
    }

    /**
     * Creates a new registrar for payloads of the given version.
     */
    public PayloadRegistrar registrar(String version) {
        return new PayloadRegistrar(version);
    }
}
