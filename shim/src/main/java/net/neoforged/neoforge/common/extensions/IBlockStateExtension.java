/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.util.BlockRelocability;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's {@code IBlockStateExtension} containing the methods used by the shared code.
 */
public interface IBlockStateExtension {
    private BlockState self() {
        return (BlockState) this;
    }

    /// Called when fire is updating, checks if a block face can catch fire.
    ///
    /// @param level The current level
    /// @param pos   Block position in level
    /// @param face  The face that the fire is coming from
    /// @return True if the face can be on fire, false otherwise.
    default boolean isFlammable(BlockGetter level, BlockPos pos, Direction face) {
        return self().getBlock().isFlammable(self(), level, pos, face);
    }

    /**
     * If the block is flammable, this is called when it gets lit on fire.
     * <p>
     * The return value determines whether a flint-and-steel in a dispenser was used successfully and should be damaged
     *
     * @param level        The current level
     * @param pos          Block position in level
     * @param face         The face that the fire is coming from
     * @param igniter      The entity that lit the fire
     * @param ignitionItem The item that was used to light the fire
     * @return whether the block was successfully set on fire (i.e. TNT is allowed to explode and was primed)
     */
    default boolean onCaughtFire(Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter, ItemStack ignitionItem) {
        return self().getBlock().onCaughtFire(self(), level, pos, face, igniter, ignitionItem);
    }

    /**
     * Returns the state that this block should transform into when right-clicked by a tool.
     * Returns {@code null} if nothing should happen.
     *
     * @param context     The use on context that the action was performed in
     * @param itemAbility The action being performed by the tool
     * @param simulate    If {@code true}, no actions that modify the world in any way should be performed. If {@code false}, the world may be modified.
     * @return The resulting state after the action has been performed
     */
    @Nullable
    default BlockState getToolModifiedState(UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        // Mekanism shim: NeoForge's tool modification event is not fired
        return self().getBlock().getToolModifiedState(self(), context, itemAbility, simulate);
    }

    /// Declares whether a block may be relocated and under what circumstances.
    ///
    /// @param level LevelReader where this blockstate is being relocated from
    /// @param pos   BlockPos of this blockstate being relocated
    /// @return BlockRelocability declaring whether the block may be relocated
    /// @see IBlockExtension#getRelocability
    default BlockRelocability getRelocability(LevelReader level, BlockPos pos) {
        return self().getBlock().getRelocability(level, pos, self());
    }

    /**
     * Called when a player removes a block. This is responsible for
     * actually destroying the block, and the block is intact at time of call.
     * This is called regardless of whether the player can harvest the block or
     * not.
     *
     * Return true if the block is actually destroyed.
     *
     * This function is called on both the logical client and logical server.
     *
     * @param level       The current level
     * @param pos         Block position in level
     * @param player      The player damaging the block, may be null
     * @param toolStack   The players main-hand prior to destroying the block and applying damage to the tool.
     * @param willHarvest The result of {@link #canHarvestBlock}, if called on the server by a non-creative player, otherwise always false.
     * @param fluid       The current fluid and block state for the position in the level.
     * @return True if the block is actually destroyed.
     */
    default boolean onDestroyedByPlayer(Level level, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        return self().getBlock().onDestroyedByPlayer(self(), level, pos, player, toolStack, willHarvest, fluid);
    }

    /**
     * Called when a block entity on a side of this block changes, is created, or is destroyed.
     *
     * <p>This method is not suitable for listening to capability invalidations.
     * For capability invalidations specifically, use {@link BlockCapabilityCache} instead.
     *
     * @param level    The level
     * @param pos      Block position in level
     * @param neighbor Block position of neighbor
     */
    default void onNeighborChange(LevelReader level, BlockPos pos, BlockPos neighbor) {
        self().getBlock().onNeighborChange(self(), level, pos, neighbor);
    }

    /**
     * If this block should be notified of weak changes.
     * Weak changes are changes 1 block away through a solid block.
     * Similar to comparators.
     *
     * @param level The current level
     * @param pos   Block position in level
     * @return true To be notified of changes
     */
    default boolean getWeakChanges(LevelReader level, BlockPos pos) {
        return self().getBlock().getWeakChanges(self(), level, pos);
    }

    /**
     * Location sensitive version of getExplosionResistance
     *
     * @param level     The current level
     * @param pos       Block position in level
     * @param explosion The explosion
     * @return The amount of the explosion absorbed.
     */
    default float getExplosionResistance(BlockGetter level, BlockPos pos, Explosion explosion) {
        return self().getBlock().getExplosionResistance(self(), level, pos, explosion);
    }

    /**
     * Get a light value for this block, taking into account the given state and coordinates, normal ranges are between 0 and 15
     */
    default int getLightEmission(BlockGetter level, BlockPos pos) {
        return self().getBlock().getLightEmission(self(), level, pos);
    }

    /**
     *
     * Called when A user uses the creative pick block button on this block
     *
     * @return A ItemStack to add to the player's inventory, empty itemstack if nothing should be added.
     */
    default ItemStack getCloneItemStack(BlockPos pos, LevelReader level, boolean includeData, Player player) {
        return self().getBlock().getCloneItemStack(level, pos, self(), includeData, player);
    }

    /**
     * Sensitive version of getSoundType
     *
     * @param level  The level
     * @param pos    The position. Note that the level may not necessarily have {@code state} here!
     * @param entity The entity that is breaking/stepping on/placing/hitting/falling on this block, or null if no entity is in this context
     * @return A SoundType to use
     */
    default SoundType getSoundType(LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return self().getBlock().getSoundType(self(), level, pos, entity);
    }

    /**
     * Gets the slipperiness at the given location at the given state. Normally between 0 and 1.
     */
    default float getFriction(LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return self().getBlock().getFriction(self(), level, pos, entity);
    }

    /**
     * @return An Integer ARGB value to be averaged with a beacon's existing beam color, or null to do nothing to the beam
     */
    @Nullable
    default Integer getBeaconColorMultiplier(LevelReader level, BlockPos pos, BlockPos beacon) {
        return self().getBlock().getBeaconColorMultiplier(self(), level, pos, beacon);
    }
}
