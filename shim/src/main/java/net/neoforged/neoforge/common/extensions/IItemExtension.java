/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantment.EnchantmentDefinition;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's {@code IItemExtension} containing the methods used by the shared code.
 */
public interface IItemExtension {
    private Item self() {
        return (Item) this;
    }

    /**
     * Checks if the provided enchantment is applicable to the passed item stack.
     * <p>
     * By default, this checks if the {@link EnchantmentDefinition#supportedItems()} contains this item,
     * special casing enchanted books as they may receive any enchantment.
     * <p>
     * Overriding this method allows for dynamic logic that would not be possible using the tag system.
     *
     * @param stack       the item stack to be enchanted
     * @param enchantment the enchantment to be applied
     * @return true if this item can accept the enchantment
     * @apiNote Call via {@link IItemStackExtension#supportsEnchantment(Holder)}
     * 
     * @see #isPrimaryItemFor(ItemStack, Holder)
     */
    @ApiStatus.OverrideOnly
    default boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.is(Items.ENCHANTED_BOOK) || enchantment.value().isSupportedItem(stack);
    }

    /**
     * Called when an item entity for this stack is destroyed. Note: The {@link ItemStack} can be retrieved from the item entity.
     *
     * @param itemEntity   The item entity that was destroyed.
     * @param damageSource Damage source that caused the item entity to "die".
     */
    default void onDestroyed(ItemEntity itemEntity, DamageSource damageSource) {
        self().onDestroyed(itemEntity);
    }

    /**
     * Called when the player is mining a block and the item in his hand changes.
     * Allows to not reset blockbreaking if only NBT or similar changes.
     *
     * @param oldStack The old stack that was used for mining. Item in players main
     *                 hand
     * @param newStack The new stack
     * @return True to reset block break progress
     */
    default boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        // Fix MC-176559 mending resets mining progress / breaking animation
        if (!newStack.is(oldStack.getItem()))
            return true;

        if (!newStack.isDamageableItem() || !oldStack.isDamageableItem())
            return !ItemStack.isSameItemSameComponents(newStack, oldStack);

        DataComponentMap newComponents = newStack.getComponents();
        DataComponentMap oldComponents = oldStack.getComponents();

        if (newComponents.isEmpty() || oldComponents.isEmpty())
            return !(newComponents.isEmpty() && oldComponents.isEmpty());

        Set<DataComponentType<?>> newKeys = new HashSet<>(newComponents.keySet());
        Set<DataComponentType<?>> oldKeys = new HashSet<>(oldComponents.keySet());

        newKeys.remove(DataComponents.DAMAGE);
        oldKeys.remove(DataComponents.DAMAGE);

        if (!newKeys.equals(oldKeys))
            return true;

        return !newKeys.stream().allMatch(key -> Objects.equals(newComponents.get(key), oldComponents.get(key)));
    }

    /**
     * Gets the level of the enchantment currently present on the stack. By default, returns the enchantment level present in NBT.
     * Most enchantment implementations rely upon this method.
     * The returned value must be the same as getting the enchantment from {@link #getAllEnchantments}
     *
     * @param stack       The item stack being checked
     * @param enchantment The enchantment being checked for
     * @return Level of the enchantment, or 0 if not present
     * @see #getAllEnchantments
     * @apiNote Call via {@link IItemStackExtension#getEnchantmentLevel}.
     */
    @ApiStatus.OverrideOnly
    default int getEnchantmentLevel(ItemInstance stack, Holder<Enchantment> enchantment) {
        ItemEnchantments itemenchantments = stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return itemenchantments.getLevel(enchantment);
    }

    /**
     * Gets a map of all enchantments present on the stack. By default, returns the enchantments present in NBT.
     * Used in several places in code including armor enchantment hooks.
     * The returned value(s) must have the same level as {@link #getEnchantmentLevel}.
     *
     * @param stack  The item stack being checked
     * @param lookup A registry lookup, used to resolve enchantment {@link Holder}s.
     * @return Map of all enchantments on the stack, empty if no enchantments are present
     * @see #getEnchantmentLevel
     * @apiNote Call via {@link IItemStackExtension#getAllEnchantments}.
     */
    @ApiStatus.OverrideOnly
    default ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    /**
     * Called by the powdered snow block to check if a living entity wearing this can walk on the snow, granting the same behavior as leather boots.
     * Only affects items worn in the boots slot.
     *
     * @param stack  Stack instance
     * @param wearer The entity wearing this ItemStack
     *
     * @return True if the entity can walk on powdered snow
     */
    default boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
        return stack.is(Items.LEATHER_BOOTS);
    }

    /**
     * Whether the given ItemStack should be excluded (if possible) when selecting the target hotbar slot of a "pick" action.
     * By default, this returns true for enchanted stacks.
     *
     * @see Inventory#getSuitableHotbarSlot()
     * @param player        the player performing the picking
     * @param inventorySlot the inventory slot of the item being up for replacement
     * @return true to leave this stack in the hotbar if possible
     */
    default boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
        return stack.isEnchanted();
    }

    /**
     * Queries if an item can perform the given action.
     * See {@link ItemAbilities} for a description of each stock action
     * 
     * @param stack       The stack being used
     * @param itemAbility The action being queried
     * @return True if the stack can perform the action
     */
    default boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
        if (itemAbility == ItemAbilities.SWORD_SWEEP) {
            return stack.is(ItemTags.SWORDS);
        } else if (itemAbility == ItemAbilities.SHOVEL_DOUSE) {
            return stack.is(ItemTags.DOUSES_CAMPFIRES);
        }
        return false;
    }

    /**
     * This is called when the item is used, before the block is activated.
     *
     * @return Return PASS to allow vanilla handling, any other to skip normal code.
     */
    default InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return InteractionResult.PASS;
    }

    /// Called to damage an item providing the [gliding flight attribute][net.neoforged.neoforge.common.NeoForgeMod#GLIDING_FLIGHT].
    /// The default vanilla implementation is to damage the item by 1.
    ///
    /// If an entity has multiple items equipped that provide the gliding flight attribute, one of those items will be randomly selected
    /// to be called with this method.
    ///
    /// @param stack  the stack of the item providing the gliding flight attribute
    /// @param wearer the entity wearing the item
    /// @param slot   the equipment slot occupied by the item
    /// @see IItemStackExtension#onGlideDamage(LivingEntity, EquipmentSlot)
    default void onGlideDamage(ItemStack stack, LivingEntity wearer, EquipmentSlot slot) {
        stack.hurtAndBreak(1, wearer, slot);
    }

    /**
     *
     * Should this item, when held, allow sneak-clicks to pass through to the
     * underlying block?
     *
     * @param level  The level
     * @param pos    Block position in level
     * @param player The Player that is wielding the item
     */
    default boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        return false;
    }

    /**
     * Called by Piglins when checking to see if they will give an item or something in exchange for this item.
     *
     * @return True if this item can be used as "currency" by piglins
     */
    default boolean isPiglinCurrency(ItemStack stack) {
        return stack.getItem() == PiglinAi.BARTERING_ITEM;
    }

    /**
     * Determines if the specific ItemStack can be placed in the specified armor
     * slot, for the entity.
     *
     * @param stack     The ItemStack
     * @param armorType Armor slot to be verified.
     * @param entity    The entity trying to equip the armor
     * @return True if the given ItemStack can be inserted in the slot
     */
    default boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
        return entity.isEquippableInSlot(stack, armorType);
    }

    /**
     * Called to get the Mod ID of the mod that *created* the ItemStack, instead of
     * the real Mod ID that *registered* it.
     *
     * For example the Forge Universal Bucket creates a subitem for each modded
     * fluid, and it returns the modded fluid's Mod ID here.
     *
     * Mods that register subitems for other mods can override this. Informational
     * mods can call it to show the mod that created the item.
     *
     * @param itemStack the ItemStack to check
     * @return the Mod ID for the ItemStack, or null when there is no specially
     *         associated mod and {@link net.minecraft.core.Registry#getKey(Object)} would return null.
     */
    @Nullable
    default String getCreatorModId(HolderLookup.Provider registries, ItemStack itemStack) {
        return CommonHooks.getDefaultCreatorModId(registries, itemStack);
    }

    /**
     * Determine if the player switching between these two item stacks
     *
     * @param oldStack    The old stack that was equipped
     * @param newStack    The new stack
     * @param slotChanged If the current equipped slot was changed, Vanilla does not play the animation if you switch between two slots that hold the exact
     *                    same item.
     * @return True to play the item change animation
     * @implNote Mekanism shim: only called (through Fabric's FabricItem#allowComponentsUpdateAnimation) when the item in the hand stayed the same
     */
    default boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack != newStack;
    }

    /**
     * Determines if the item can be damaged.
     *
     * @param stack The stack to check
     * @return true if the item can be damaged
     * @implNote Mekanism shim: only checked by the repair crafting recipe (like NeoForge), not the grindstone
     */
    default boolean isDamageable(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.MAX_DAMAGE);
    }
}
