/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.client.extensions;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.util.context.ContextMap;

/**
 * Mekanism shim: Subset of NeoForge's {@code UnbakedModelExtension}, injected into {@link UnbakedModel}.
 */
public interface UnbakedModelExtension {

    /**
     * Appends additional properties for this model to the builder.
     *
     * @apiNote Mekanism shim: Additional properties are only used by the model geometry that requests them.
     */
    default void fillAdditionalProperties(ContextMap.Builder propertiesBuilder) {
    }
}
