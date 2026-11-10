/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's {@code IItemStackExtension}, injected into {@link ItemStack}.
 */
public interface IItemStackExtension {

    default <T, C extends @Nullable Object> @Nullable T getCapability(ItemCapability<T, C> capability, C context) {
        return capability.getCapability((ItemStack) (Object) this, context);
    }

    default <T> @Nullable T getCapability(ItemCapability<T, @Nullable Void> capability) {
        return capability.getCapability((ItemStack) (Object) this, null);
    }
}
