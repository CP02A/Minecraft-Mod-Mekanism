/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;

/**
 * Mekanism shim: The display hook NeoForge patches into {@link RecipeButton}, injected as an interface.
 */
public interface IRecipeButtonExtension {

    /**
     * Renders the displayed result of the recipe button.
     *
     * @apiNote Mekanism shim: Vanilla's rendering does not call this, the loader redirects to it.
     */
    default void extractDisplayRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, boolean displayMultiple) {
    }
}
