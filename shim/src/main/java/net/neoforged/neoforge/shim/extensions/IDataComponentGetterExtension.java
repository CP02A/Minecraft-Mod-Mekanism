/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: The supplier based overloads NeoForge patches directly into {@link DataComponentGetter}, injected as an interface.
 */
public interface IDataComponentGetterExtension {

    private DataComponentGetter self() {
        return (DataComponentGetter) this;
    }

    default <T> @Nullable T get(Supplier<? extends DataComponentType<? extends T>> componentType) {
        return self().get(componentType.get());
    }

    default <T> T getOrDefault(Supplier<? extends DataComponentType<? extends T>> componentType, T value) {
        return self().getOrDefault(componentType.get(), value);
    }

    default <T> boolean has(Supplier<? extends DataComponentType<? extends T>> componentType) {
        return self().get(componentType.get()) != null;
    }
}
