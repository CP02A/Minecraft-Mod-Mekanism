/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: The level accessor NeoForge patches into {@link Item.TooltipContext}, injected as an interface.
 */
public interface ITooltipContextExtension {

    /**
     * {@return the level the tooltip is being displayed in, or {@code null} if it is not known}
     */
    default @Nullable Level level() {
        return null;
    }
}
