/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: The custom sprite hook NeoForge patches into {@link ChestRenderer}, injected as an interface.
 */
public interface IChestRendererExtension<T> {

    /**
     * {@return a custom sprite to render the chest with, or {@code null} to use the default one}
     */
    default @Nullable SpriteId getCustomSprite(T blockEntity, ChestRenderState renderState) {
        return null;
    }
}
