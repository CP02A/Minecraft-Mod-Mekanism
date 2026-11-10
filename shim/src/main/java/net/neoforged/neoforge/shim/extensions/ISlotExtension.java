/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.shim.ShimHooks;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link Slot}, injected as an interface.
 */
public interface ISlotExtension {

    /**
     * {@return the index of this slot in its container}
     */
    default int getSlotIndex() {
        return ((Slot) this).getContainerSlot();
    }

    /**
     * Sets the sprite rendered in this slot while it is empty.
     */
    default Slot setBackground(Identifier sprite) {
        ShimHooks.setSlotBackground((Slot) this, sprite);
        return (Slot) this;
    }
}
