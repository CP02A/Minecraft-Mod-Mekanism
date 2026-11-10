/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: The slot rendering hook NeoForge patches into {@link AbstractContainerScreen}, injected as an interface.
 */
public interface IAbstractContainerScreenExtension {

    /**
     * Renders the contents of a slot.
     *
     * @apiNote Mekanism shim: Vanilla's slot rendering does not call this, the loader redirects to it.
     */
    default void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack itemStack, Slot slot, @Nullable String itemCount) {
        int seed = slot.x + slot.y * ((AbstractContainerScreen<?>) this).imageWidth;
        if (slot.isFake()) {
            graphics.fakeItem(itemStack, slot.x, slot.y, seed);
        } else {
            graphics.item(itemStack, slot.x, slot.y, seed);
        }
        graphics.itemDecorations(Minecraft.getInstance().font, itemStack, slot.x, slot.y, itemCount);
    }
}
