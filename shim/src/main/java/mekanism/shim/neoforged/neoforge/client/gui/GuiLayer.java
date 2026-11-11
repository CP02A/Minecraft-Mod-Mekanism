/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.neoforge.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@FunctionalInterface
public interface GuiLayer {
    void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker);
}
