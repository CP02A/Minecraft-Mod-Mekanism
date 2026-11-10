/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.shim.ShimServices;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link Ingredient}, injected as an interface.
 *
 * @implNote {@code getCustomIngredient()} is intentionally not provided, as it would clash with Fabric's method of the same name. Use
 * {@link net.neoforged.neoforge.shim.ShimBackend#getCustomIngredient(Ingredient)} instead.
 */
public interface IIngredientExtension {

    private Ingredient self() {
        return (Ingredient) (Object) this;
    }

    /**
     * {@return {@code true} if this ingredient is backed by a NeoForge style custom ingredient}
     */
    default boolean isCustom() {
        return ShimServices.BACKEND.getCustomIngredient(self()) != null;
    }

    /**
     * {@return {@code true} if this ingredient only depends on the item, and not on any components of the stack}
     */
    default boolean isSimple() {
        return ShimServices.BACKEND.isSimple(self());
    }

    /**
     * {@return the items this (non custom) ingredient matches}
     *
     * @throws IllegalStateException If this is a custom ingredient.
     */
    default HolderSet<Item> getValues() {
        if (isCustom()) {
            throw new IllegalStateException("Cannot retrieve values from custom ingredient!");
        }
        return self().values;
    }
}
