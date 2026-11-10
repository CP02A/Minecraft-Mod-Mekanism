/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link DyeColor}, injected as an interface.
 */
public interface IDyeColorExtension {

    /**
     * {@return the tag key representing the set of items which provide this dye color}
     */
    default TagKey<Item> getTag() {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dyes/" + ((DyeColor) (Object) this).getSerializedName()));
    }

    /**
     * {@return the tag key representing the set of items which are dyed with this color}
     */
    default TagKey<Item> getDyedTag() {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "dyed/" + ((DyeColor) (Object) this).getSerializedName()));
    }
}
