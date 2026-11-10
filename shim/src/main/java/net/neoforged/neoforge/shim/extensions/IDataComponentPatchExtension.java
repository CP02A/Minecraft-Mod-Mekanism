/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import java.util.Set;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link DataComponentPatch}, injected as an interface.
 */
public interface IDataComponentPatchExtension {

    private DataComponentPatch self() {
        return (DataComponentPatch) (Object) this;
    }

    /**
     * {@return {@code true} if this patch either sets or removes the given component}
     */
    default boolean isPatched(DataComponentType<?> type) {
        return self().map.containsKey(type);
    }

    /**
     * {@return the patch stored for the given component type, {@code null} if the patch removes the component}
     *
     * @throws IllegalStateException If the component is not patched.
     */
    default <T> @Nullable T getPatch(DataComponentType<? extends T> type) {
        if (!isPatched(type)) {
            throw new IllegalStateException("DataComponentType " + type + " is not patched");
        }
        //Getting with an empty prototype returns null for removed components, and the value for set ones
        return self().get(DataComponentMap.EMPTY, type);
    }

    /**
     * {@return all the component types this patch sets or removes}
     */
    default Set<DataComponentType<?>> keySet() {
        return self().map.keySet();
    }
}
