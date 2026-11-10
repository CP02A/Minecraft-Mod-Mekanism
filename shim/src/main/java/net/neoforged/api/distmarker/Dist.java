/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.api.distmarker;

/**
 * The physical distribution the game is running on.
 *
 * <p>Mekanism shim: Equivalent of NeoForge's dist marker.
 */
public enum Dist {
    CLIENT,
    DEDICATED_SERVER;

    public boolean isDedicatedServer() {
        return !isClient();
    }

    public boolean isClient() {
        return this == CLIENT;
    }
}
