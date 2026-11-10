/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.shim.ShimHooks;

/**
 * Mekanism shim: Subset of NeoForge's {@code IFluidExtension}, injected into {@link Fluid}.
 */
public interface IFluidExtension {

    /**
     * {@return the fluid type of this fluid}
     */
    default FluidType getFluidType() {
        return ShimHooks.getFluidType((Fluid) this);
    }
}
