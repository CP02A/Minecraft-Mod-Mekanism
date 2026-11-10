/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.data.loading;

/**
 * Mekanism shim: Data generation only runs on NeoForge.
 */
public class DatagenModLoader {

    public static boolean isRunningDataGen() {
        return false;
    }
}
