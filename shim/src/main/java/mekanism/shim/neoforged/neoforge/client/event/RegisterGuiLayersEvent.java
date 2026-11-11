/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.neoforge.client.event;

import mekanism.shim.neoforged.neoforge.client.gui.GuiLayer;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import mekanism.shim.neoforged.fml.event.IModBusEvent;
import mekanism.shim.neoforged.neoforge.shim.ShimServices;

/**
 * Fired to register HUD layers.
 *
 * <p>Mekanism shim: Layers are registered with the loader's HUD rendering API.
 */
public class RegisterGuiLayersEvent extends Event implements IModBusEvent {

    public RegisterGuiLayersEvent() {
    }

    public void registerBelowAll(Identifier id, GuiLayer layer) {
        ShimServices.CLIENT.registerGuiLayer(id, layer, null, false);
    }

    public void registerBelow(Identifier other, Identifier id, GuiLayer layer) {
        ShimServices.CLIENT.registerGuiLayer(id, layer, other, false);
    }

    public void registerAbove(Identifier other, Identifier id, GuiLayer layer) {
        ShimServices.CLIENT.registerGuiLayer(id, layer, other, true);
    }

    public void registerAboveAll(Identifier id, GuiLayer layer) {
        ShimServices.CLIENT.registerGuiLayer(id, layer, null, true);
    }
}
