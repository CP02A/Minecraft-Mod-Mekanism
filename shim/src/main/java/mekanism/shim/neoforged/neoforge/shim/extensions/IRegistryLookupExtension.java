/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.neoforge.shim.extensions;

import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import mekanism.shim.neoforged.neoforge.registries.datamaps.DataMapType;
import mekanism.shim.neoforged.neoforge.shim.DataMapStorage;

/**
 * Mekanism shim: The data map accessor NeoForge patches into {@link HolderLookup.RegistryLookup}, injected as an interface.
 */
public interface IRegistryLookupExtension<T> {

    default <A> Map<ResourceKey<T>, A> getDataMap(DataMapType<T, A> type) {
        return DataMapStorage.getDataMap(type);
    }
}
