/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;

/**
 * Mekanism shim: The overload NeoForge patches into {@link BlocksAttacks}, injected as an interface.
 */
public interface IBlocksAttacksExtension {

    /**
     * Damages the blocking item.
     *
     * @param itemDamage Mekanism shim: NeoForge allows specifying the item damage directly; otherwise vanilla computes it from the blocked damage.
     */
    default void hurtBlockingItem(Level level, ItemStack item, LivingEntity user, InteractionHand hand, float damage, int itemDamage) {
        ((BlocksAttacks) (Object) this).hurtBlockingItem(level, item, user, hand, damage);
    }
}
