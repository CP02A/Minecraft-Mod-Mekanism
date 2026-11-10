/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Mekanism shim: The subset of FML's mod loader used by the shared code.
 */
public final class ModLoader {

    private ModLoader() {
    }

    /**
     * Posts the event to the event bus of every mod using the shim.
     */
    public static <T extends Event & IModBusEvent> void postEvent(T event) {
        for (ModContainer container : ModList.get().getModContainers()) {
            container.getEventBus().post(event);
        }
    }
}
