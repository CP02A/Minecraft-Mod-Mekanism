/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Mekanism shim: Subset of NeoForge's {@code ILivingEntityExtension} containing the methods used by the shared code.
 */
public interface ILivingEntityExtension extends IEntityExtension {
    default LivingEntity self() {
        return (LivingEntity) this;
    }

    /**
     * Executes in {@link LivingEntity#hurt(DamageSource, float)} after all damage and
     * effects have applied. Overriding this method is preferred over overriding the
     * hurt method in custom entities where special behavior is desired after vanilla
     * logic.
     *
     * @param damageContainer The aggregated damage details preceding this hook, which
     *                        includes changes made to the damage sequence by events.
     */
    default void onDamageTaken(DamageContainer damageContainer) {}
}
