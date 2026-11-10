/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common;

import java.util.Objects;
import java.util.UUID;
import net.neoforged.neoforge.shim.ShimServices;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Looks up last known usernames through the loader.
 */
public final class UsernameCache {

    private UsernameCache() {
    }

    @Nullable
    public static String getLastKnownUsername(UUID uuid) {
        Objects.requireNonNull(uuid);
        return ShimServices.BACKEND.getLastKnownUsername(uuid);
    }
}
