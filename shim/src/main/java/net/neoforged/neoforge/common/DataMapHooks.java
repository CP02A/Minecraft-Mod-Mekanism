/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common;

import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;

/**
 * Mekanism shim: NeoForge allows extending block transformers with data maps; this is not supported by the shim.
 */
public class DataMapHooks {

    public static Iterable<BlockTransformer.BlockTransformData> getAllTransformers(Holder<BlockTransformer> component) {
        return component.value().transforms();
    }
}
