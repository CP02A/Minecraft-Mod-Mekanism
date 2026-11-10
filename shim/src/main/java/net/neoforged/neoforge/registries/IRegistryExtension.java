/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.shim.DataMapStorage;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's {@code IRegistryExtension}, injected into {@link Registry}.
 */
public interface IRegistryExtension<T> {

    /**
     * {@return the key of the given element, or {@code null} if it is not registered}
     */
    @SuppressWarnings("unchecked")
    default @Nullable Identifier getKeyOrNull(T element) {
        return ((Registry<T>) this).getKey(element);
    }

    /**
     * {@return all values of the given data map in this registry}
     */
    default <A> Map<ResourceKey<T>, A> getDataMap(DataMapType<T, A> type) {
        return DataMapStorage.getDataMap(type);
    }

    /**
     * {@return the data map value attached to the given key, or {@code null} if there is none}
     */
    default <A> @Nullable A getData(DataMapType<T, A> type, ResourceKey<T> key) {
        return DataMapStorage.getData(type, key);
    }
}
