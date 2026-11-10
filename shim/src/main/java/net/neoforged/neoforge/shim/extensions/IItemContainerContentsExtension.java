/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link ItemContainerContents}, injected as an interface.
 */
public interface IItemContainerContentsExtension {

    private ItemContainerContents self() {
        return (ItemContainerContents) (Object) this;
    }

    /**
     * {@return the number of slots in this container}
     */
    default int getSlots() {
        return self().items.size();
    }

    /**
     * {@return the item stack template at the given slot}
     *
     * @throws UnsupportedOperationException If the slot index is invalid.
     */
    default Optional<ItemStackTemplate> getTemplateInSlot(int slot) {
        if (slot < 0 || slot >= getSlots()) {
            throw new UnsupportedOperationException("Slot " + slot + " not in valid range - [0," + getSlots() + ")");
        }
        return self().items.get(slot);
    }

    /**
     * {@return a copy of the item stack at the given slot}
     */
    default ItemStack getStackInSlot(int slot) {
        return getTemplateInSlot(slot).map(ItemStackTemplate::create).orElse(ItemStack.EMPTY);
    }
}
