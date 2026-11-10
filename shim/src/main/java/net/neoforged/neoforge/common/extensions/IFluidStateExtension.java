/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Mekanism shim: Subset of NeoForge's {@code IFluidStateExtension}, injected into {@link FluidState}.
 */
public interface IFluidStateExtension {

    /**
     * {@return the fluid type of this state's fluid}
     */
    default FluidType getFluidType() {
        return ((FluidState) this).getType().getFluidType();
    }
}
