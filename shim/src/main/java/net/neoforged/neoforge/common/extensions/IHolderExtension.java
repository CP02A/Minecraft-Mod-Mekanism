/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.IWithData;
import net.neoforged.neoforge.shim.DataMapStorage;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's {@code IHolderExtension}, injected into {@link Holder}.
 */
public interface IHolderExtension<T> extends IWithData<T> {

    private Holder<T> self() {
        return (Holder<T>) this;
    }

    /**
     * {@return the resource key of this holder}
     *
     * @throws IllegalStateException If this holder does not have a key.
     */
    default ResourceKey<T> getKey() {
        return self().unwrapKey().orElseThrow(() -> new IllegalStateException("Holder " + this + " does not have a key"));
    }

    /**
     * {@return the resource key of this holder, or {@code null} if it does not have one}
     */
    default @Nullable ResourceKey<T> keyOrNull() {
        return self().unwrapKey().orElse(null);
    }

    /**
     * {@return the data map value attached to this holder, or {@code null} if there is none}
     */
    @Override
    default <D> @Nullable D getData(DataMapType<T, D> type) {
        return DataMapStorage.getData(type, keyOrNull());
    }
}
