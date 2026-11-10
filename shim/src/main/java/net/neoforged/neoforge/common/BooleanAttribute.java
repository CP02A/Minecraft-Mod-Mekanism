/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * An attribute that represents a boolean: values greater than zero are {@code true}.
 *
 * <p>Mekanism shim: Implemented as a ranged attribute between 0 and 1; NeoForge's tooltip formatting is not supported.
 */
public class BooleanAttribute extends RangedAttribute {

    public BooleanAttribute(String descriptionId, boolean defaultValue) {
        super(descriptionId, defaultValue ? 1 : 0, 0, 1);
    }
}
