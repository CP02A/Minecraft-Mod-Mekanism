/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Mekanism shim: The tab ordering methods NeoForge patches directly into {@link CreativeModeTab.Builder}, injected as an interface.
 * <p>
 * Fabric has no creative tab sorting, modded tabs are shown in registration order (which already places tabs of addons after the tabs of the mods they
 * depend on), so these are no-ops.
 */
public interface ICreativeModeTabBuilderExtension {

    private CreativeModeTab.Builder self() {
        return (CreativeModeTab.Builder) (Object) this;
    }

    /**
     * Sets the tabs that should be before this tab.
     */
    default CreativeModeTab.Builder withTabsBefore(Identifier... tabs) {
        return self();
    }

    /**
     * Sets the tabs that should be after this tab.
     */
    default CreativeModeTab.Builder withTabsAfter(Identifier... tabs) {
        return self();
    }

    /**
     * Sets the tabs that should be before this tab.
     */
    @SuppressWarnings("unchecked")
    default CreativeModeTab.Builder withTabsBefore(ResourceKey<CreativeModeTab>... tabs) {
        return self();
    }

    /**
     * Sets the tabs that should be after this tab.
     */
    @SuppressWarnings("unchecked")
    default CreativeModeTab.Builder withTabsAfter(ResourceKey<CreativeModeTab>... tabs) {
        return self();
    }
}
