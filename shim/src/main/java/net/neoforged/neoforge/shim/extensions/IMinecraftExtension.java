/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.neoforged.neoforge.shim.ShimHooks;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link Minecraft}, injected as an interface.
 */
public interface IMinecraftExtension {

    /**
     * {@return a block model resolver backed by the game's model manager}
     *
     * @apiNote Mekanism shim: Vanilla does not keep its resolver around, so the shim keeps its own instance.
     */
    default BlockModelResolver getBlockModelResolver() {
        return ShimHooks.getBlockModelResolver((Minecraft) this);
    }
}
