/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

/**
 * The logical side code is running on.
 *
 * <p>Mekanism shim: Equivalent of FML's logical side.
 */
public enum LogicalSide {
    CLIENT,
    SERVER;

    public boolean isServer() {
        return !isClient();
    }

    public boolean isClient() {
        return this == CLIENT;
    }
}
