/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import java.util.stream.Stream;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link EntityType}, injected as an interface.
 */
public interface IEntityTypeExtension {

    default Stream<TagKey<EntityType<?>>> getTags() {
        return ((EntityType<?>) this).builtInRegistryHolder().tags();
    }
}
