/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link RecipeManager}, injected as an interface.
 */
public interface IRecipeManagerExtension {

    default RecipeMap recipeMap() {
        return ((RecipeManager) this).recipes;
    }
}
