/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.neoforge.network.registration;

/**
 * The thread a payload handler is invoked on.
 */
public enum HandlerThread {
    /**
     * The main thread of the receiving side.
     */
    MAIN,
    /**
     * The network thread the payload was received on.
     */
    NETWORK
}
