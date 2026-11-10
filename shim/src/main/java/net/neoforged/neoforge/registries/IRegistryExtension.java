/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.shim.DataMapStorage;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's {@code IRegistryExtension}, injected into {@link Registry}.
 */
public interface IRegistryExtension<T> extends net.neoforged.neoforge.shim.extensions.IRegistryLookupExtension<T> {

    /**
     * {@return the key of the given element, or {@code null} if it is not registered}
     */
    @SuppressWarnings("unchecked")
    default @Nullable Identifier getKeyOrNull(T element) {
        return ((Registry<T>) this).getKey(element);
    }

    // Mekanism shim: getDataMap is inherited from IRegistryLookupExtension, as registries are also registry lookups (which would otherwise have
    // conflicting default methods)

    /**
     * {@return the data map value attached to the given key, or {@code null} if there is none}
     */
    default <A> @Nullable A getData(DataMapType<T, A> type, ResourceKey<T> key) {
        return DataMapStorage.getData(type, key);
    }
}
