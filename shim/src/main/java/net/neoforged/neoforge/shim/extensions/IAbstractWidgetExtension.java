/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Mekanism shim: The label color hook NeoForge patches into {@link AbstractWidget}, injected as an interface.
 */
public interface IAbstractWidgetExtension {

    /**
     * {@return the color of the widget's label}
     */
    default int getLabelTextColor() {
        return ((AbstractWidget) this).active ? 0xFFFFFFFF : 0xFFA0A0A0;
    }
}
