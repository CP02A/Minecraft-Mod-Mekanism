/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim;

import java.util.function.Supplier;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Client side functionality the shim needs from the loader.
 */
public interface ClientBackend {

    void registerLayerDefinition(ModelLayerLocation layer, Supplier<LayerDefinition> definition);

    /**
     * Registers a HUD layer.
     *
     * @param relativeTo The layer to register relative to, or {@code null} to register relative to all layers.
     * @param above      {@code true} to render above the other layer(s), {@code false} to render below.
     */
    void registerGuiLayer(Identifier id, GuiLayer layer, @Nullable Identifier relativeTo, boolean above);
}
