/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jspecify.annotations.Nullable;

/**
 * Stores the loaded data maps of every registry, which NeoForge stores in the registries themselves. Populated by the loader specific data map
 * loader (and data map syncing on the client).
 */
public final class DataMapStorage {

    private DataMapStorage() {
    }

    private static final Map<ResourceKey<? extends Registry<?>>, Map<DataMapType<?, ?>, Map<ResourceKey<?>, ?>>> DATA_MAPS = new ConcurrentHashMap<>();

    /**
     * Replaces the contents of the given data map.
     */
    public static <R, T> void set(DataMapType<R, T> type, Map<ResourceKey<R>, T> values) {
        DATA_MAPS.computeIfAbsent(type.registryKey(), k -> new ConcurrentHashMap<>()).put(type, Map.copyOf(values));
    }

    /**
     * Clears all loaded data maps, for example when leaving a server.
     */
    public static void clear() {
        DATA_MAPS.clear();
    }

    /**
     * {@return all values of the given data map}
     */
    @SuppressWarnings("unchecked")
    public static <R, T> Map<ResourceKey<R>, T> getDataMap(DataMapType<R, T> type) {
        Map<DataMapType<?, ?>, Map<ResourceKey<?>, ?>> registryMaps = DATA_MAPS.get(type.registryKey());
        if (registryMaps == null) {
            return Map.of();
        }
        Map<ResourceKey<?>, ?> values = registryMaps.get(type);
        return values == null ? Map.of() : (Map<ResourceKey<R>, T>) (Map<?, ?>) values;
    }

    /**
     * {@return the value of the given data map for the given key, or {@code null} if there is none}
     */
    public static <R, T> @Nullable T getData(DataMapType<R, T> type, @Nullable ResourceKey<R> key) {
        return key == null ? null : getDataMap(type).get(key);
    }
}
