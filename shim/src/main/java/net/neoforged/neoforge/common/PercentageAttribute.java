/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * An attribute whose value is displayed as a percentage.
 *
 * <p>Mekanism shim: NeoForge's tooltip formatting for percentage attributes is not supported.
 */
public class PercentageAttribute extends RangedAttribute {

    public PercentageAttribute(String descriptionId, double defaultValue, double min, double max) {
        super(descriptionId, defaultValue, min, max);
    }
}
