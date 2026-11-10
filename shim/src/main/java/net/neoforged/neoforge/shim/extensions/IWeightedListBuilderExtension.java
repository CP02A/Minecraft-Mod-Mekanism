/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import java.util.List;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link WeightedList.Builder}, injected as an interface.
 * <p>
 * Vanilla's builder is backed by an immutable list builder, so removing entries is not supported.
 */
public interface IWeightedListBuilderExtension<E> {

    @SuppressWarnings("unchecked")
    private WeightedList.Builder<E> self() {
        return (WeightedList.Builder<E>) (Object) this;
    }

    /**
     * Adds a weighted entry.
     */
    default WeightedList.Builder<E> add(Weighted<E> value) {
        self().result.add(value);
        return self();
    }

    /**
     * {@return a snapshot of the entries added so far} (NeoForge returns an unmodifiable view)
     */
    default List<Weighted<E>> getList() {
        return self().result.build();
    }
}
