/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.capabilities.EntityCapability;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's entity extensions, injected into {@link Entity}.
 */
public interface IEntityExtension {

    default <T, C extends @Nullable Object> @Nullable T getCapability(EntityCapability<T, C> capability, C context) {
        return capability.getCapability((Entity) this, context);
    }

    default <T> @Nullable T getCapability(EntityCapability<T, @Nullable Void> capability) {
        return capability.getCapability((Entity) this, null);
    }
}
