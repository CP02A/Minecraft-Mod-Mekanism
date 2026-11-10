/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

/**
 * Mekanism shim: Subset of NeoForge's {@code ValueOutputExtension} containing the methods used by the shared code.
 */
public interface ValueOutputExtension {
    private ValueOutput self() {
        return (ValueOutput) this;
    }

    /**
     * Store the given {@code child} object at the given {@code key}.
     *
     * @param key   the key to put the child at
     * @param child the child to store at the given key
     */
    default void putChild(String key, ValueIOSerializable child) {
        child.serialize(self().child(key));
    }
}
