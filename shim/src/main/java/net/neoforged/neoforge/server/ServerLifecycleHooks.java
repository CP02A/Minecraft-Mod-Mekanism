/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.server;

import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's server lifecycle hooks. The current server is tracked by the loader.
 */
public class ServerLifecycleHooks {

    @Nullable
    private static MinecraftServer currentServer;

    @Nullable
    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }

    /**
     * @apiNote Mekanism shim: Called by the loader when a server starts and stops.
     */
    public static void setCurrentServer(@Nullable MinecraftServer server) {
        currentServer = server;
    }
}
