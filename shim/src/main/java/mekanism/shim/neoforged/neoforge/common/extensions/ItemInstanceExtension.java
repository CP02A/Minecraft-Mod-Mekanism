/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.neoforge.common.extensions;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import mekanism.shim.neoforged.neoforge.common.ItemAbilities;
import mekanism.shim.neoforged.neoforge.common.ItemAbility;
import mekanism.shim.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's {@code ItemInstanceExtension} containing the methods used by the shared code.
 */
public interface ItemInstanceExtension {
    private ItemInstance self() {
        return (ItemInstance) this;
    }

    /**
     * Queries if an item can perform the given action.
     * See {@link ItemAbilities} for a description of each stock action
     *
     * @param itemAbility The action being queried
     * @return True if the stack can perform the action
     */
    default boolean canPerformAction(ItemAbility itemAbility) {
        return self().typeHolder().value().canPerformAction(self(), itemAbility);
    }

    /**
     * ItemInstance sensitive version of {@link Item#getCraftingRemainder()}.
     * Returns a full ItemStackTemplate instance of the result.
     *
     * @return The resulting ItemStackTemplate
     */
    default @Nullable ItemStackTemplate getCraftingRemainder() {
        return self().typeHolder().value().getCraftingRemainder();
    }
}
