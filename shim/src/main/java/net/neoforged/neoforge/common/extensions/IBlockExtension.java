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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.BlockRelocability;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's {@code IBlockExtension} containing the methods used by the shared code.
 */
public interface IBlockExtension {
    private Block self() {
        return (Block) this;
    }

    /**
     * Called when A user uses the creative pick block button on this block
     *
     * @return A ItemStack to add to the player's inventory, empty itemstack if nothing should be added.
     */
    default ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return state.getCloneItemStack(level, pos, includeData);
    }

    /// Declares whether a block may be relocated and under what circumstances.
    /// "Relocation" here means a region of blocks being cut or copied,
    /// and pasted somewhere else, with a translation and possibly a rotation or mirror,
    /// also copying any blockentity data to the new position(s).
    ///
    /// A multiblock which is relocatable if and only if the entire multiblock is being relocated
    /// (e.g. a bed, a door, a 3x3 machine) may override this method to define such behavior.
    ///
    /// Blocks which may never be relocated should be added to
    /// {@link Tags.Blocks#RELOCATION_NOT_SUPPORTED}, in which case this method does not need to be overridden.
    ///
    /// @param level LevelReader which the block is being relocated from
    /// @param pos   BlockPos which the block is being relocated from
    /// @param state BlockState of the block being relocated
    /// @return BlockRelocability declaring whether the block may be relocated
    default BlockRelocability getRelocability(LevelReader level, BlockPos pos, BlockState state) {
        return state.is(Tags.Blocks.RELOCATION_NOT_SUPPORTED)
                ? BlockRelocability.No.INSTANCE
                : BlockRelocability.Yes.INSTANCE;
    }

    /**
     * Location sensitive version of getExplosionResistance
     *
     * @param level     The current level
     * @param pos       Block position in level
     * @param explosion The explosion
     * @return The amount of the explosion absorbed.
     */
    default float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return self().getExplosionResistance();
    }

    /**
     * Returns the reaction of the block when pushed or pulled by a piston. This method should be not called directly, instead via {@link BlockState#getPistonPushReaction()}.
     * <ul>
     * <li>NORMAL: is pushable and pullable by sticky pistons</li>
     * <li>DESTROY: is being destroyed on pushing and pulling</li>
     * <li>BLOCK: is not being able to be moved</li>
     * <li>IGNORE: only usable by entities</li>
     * <li>PUSH_ONLY: can only be pushed, blocks on trying to be pulled</li>
     * <li>{@code null}: use the PistonPushReaction from the BlockBehaviour.Properties passed into the Block Constructor</li>
     * </ul>
     *
     * @param state The state of this block
     * @return the PushReaction of this state or {@code null} if the one passed into the block properties should be used
     */
    @Nullable
    default PushReaction getPistonPushReaction(BlockState state) {
        return null;
    }

    /**
     * Called when the block is destroyed by an explosion.
     * Useful for allowing the block to take into account tile entities,
     * state, etc. when exploded, before it is removed.
     *
     * @param level     The current level
     * @param pos       Block position in level
     * @param explosion The explosion instance affecting the block
     */
    default void onBlockExploded(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        self().wasExploded(level, pos, explosion);
    }

    default BlockState rotate(BlockState state, LevelAccessor level, BlockPos pos, Rotation direction) {
        return state.rotate(direction);
    }

    /**
     * Gets the path type of this block when an entity is pathfinding. When
     * {@code null}, uses vanilla behavior.
     *
     * @param state the state of the block
     * @param level the level which contains this block
     * @param pos   the position of the block
     * @param mob   the mob currently pathfinding, may be {@code null}
     * @return the path type of this block
     */
    @Nullable
    default PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return state.getBlock() == Blocks.LAVA ? PathType.LAVA : (state.is(net.minecraft.tags.BlockTags.FIRE) || state.is(Blocks.LAVA)) ? PathType.FIRE : null;
    }

    /**
     * Get a light value for this block, taking into account the given state and coordinates, normal ranges are between 0 and 15
     *
     * @param state The state of this block
     * @param level The level this block is in, may be {@link EmptyBlockGetter#INSTANCE}, see implementation notes
     * @param pos   The position of this block in the level, may be {@link BlockPos#ZERO}, see implementation notes
     * @return The light value
     * @implNote <ul>
     *           <li>
     *           If the given state of this block may emit light but requires position context to determine the light
     *           value, then it must return {@code true} from {@link #hasDynamicLightEmission(BlockState)}, otherwise
     *           this method will be called with {@link EmptyBlockGetter#INSTANCE} and {@link BlockPos#ZERO} during
     *           chunk generation or loading to determine whether a chunk may contain a light-emitting block,
     *           resulting in erroneous data if it's determined with the given level and/or the given position.
     *           </li>
     *           <li>
     *           The given {@link BlockGetter} may be a chunk. Block, fluid or block entity accesses outside of its bounds
     *           will cause issues such as wrapping coordinates returning values from the opposing chunk edge
     *           </li>
     *           <li>
     *           If the light value depends on data from a {@link BlockEntity} then the light level must be stored in
     *           the {@code AuxiliaryLightManager} by the {@code BlockEntity} and retrieved from the
     *           {@code AuxiliaryLightManager} in this method. This is to ensure thread-safety and availability of
     *           the data during chunk load from disk.
     *           </li>
     *           </ul>
     */
    default int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getLightEmission();
    }

    /**
     * Called to determine whether this block should use the fluid overlay texture or flowing texture when it is placed under the fluid.
     *
     * @param state      The current state
     * @param level      The level
     * @param pos        Block position in level
     * @param fluidState The state of the fluid
     * @return Whether the fluid overlay texture should be used
     */
    default boolean shouldDisplayFluidOverlay(BlockState state, BlockAndLightGetter level, BlockPos pos, FluidState fluidState) {
        return state.getBlock() instanceof HalfTransparentBlock || state.getBlock() instanceof LeavesBlock;
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
     * @param state       The current state.
     * @param level       The current level
     * @param pos         Block position in level
     * @param player      The player damaging the block, may be null
     * @param toolStack   The players main-hand prior to destroying the block and applying damage to the tool.
     * @param willHarvest The result of {@link #canHarvestBlock}, if called on the server by a non-creative player, otherwise always false.
     * @param fluid       The current fluid state at current position
     * @return True if the block is actually destroyed.
     */
    default boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        if (level.isClientSide()) {
            // On the client, vanilla calls Level#setBlock, per MultiPlayerGameMode#destroyBlock
            return level.setBlock(pos, fluid.createLegacyBlock(), 11);
        } else {
            // On the server, vanilla calls Level#removeBlock, per ServerPlayerGameMode#destroyBlock
            return level.removeBlock(pos, false);
        }
    }

    /**
     * Returns how many experience points this block drops when broken, before application of {@linkplain EnchantmentEffectComponents#BLOCK_EXPERIENCE enchantments}.
     *
     * @param state       The state of the block being broken
     * @param level       The level
     * @param pos         The position of the block being broken
     * @param blockEntity The block entity, if any
     * @param breaker     The entity who broke the block, if known
     * @param tool        The item stack used to break the block. May be empty
     * @return The amount of experience points dropped by this block
     */
    default int getExpDrop(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker, ItemStack tool) {
        return 0;
    }

    /// Called when fire is updating, checks if a block face can catch fire.
    ///
    /// @param state     The current state
    /// @param level     The current level
    /// @param pos       Block position in level
    /// @param direction The direction that the fire is coming from
    /// @return True if the face can be on fire, false otherwise.
    default boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return ((net.minecraft.world.level.block.FireBlock) Blocks.FIRE).getIgniteOdds(state) > 0;
    }

    /**
     * If the block is flammable, this is called when it gets lit on fire.
     * <p>
     * The return value determines whether a flint-and-steel in a dispenser was used successfully and should be damaged
     *
     * @param state        The current state
     * @param level        The current level
     * @param pos          Block position in level
     * @param direction    The direction that the fire is coming from
     * @param igniter      The entity that lit the fire
     * @param ignitionItem The item that was used to light the fire
     * @return whether the block was successfully set on fire (i.e. TNT is allowed to explode and was primed)
     */
    default boolean onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter, ItemStack ignitionItem) {
        return true;
    }

    /**
     * Returns the state that this block should transform into when right-clicked by a tool.
     * Returns {@code null} if nothing should happen.
     *
     * @param state       The current state
     * @param context     The use on context that the action was performed in
     * @param itemAbility The action being performed by the tool
     * @param simulate    If {@code true}, no actions that modify the world in any way should be performed. If {@code false}, the world may be modified.
     * @return The resulting state after the action has been performed
     */
    @Nullable
    default BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
        ItemStack itemStack = context.getItemInHand();
        if (!itemStack.canPerformAction(itemAbility)) {
            return null;
        }

        if (ItemAbilities.SHEARS_TRIM == itemAbility) {
            if (state.getBlock() instanceof GrowingPlantHeadBlock growingPlant && !growingPlant.isMaxAge(state)) {
                if (!simulate) {
                    context.getLevel().playSound(context.getPlayer(), context.getClickedPos(), SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return growingPlant.getMaxAgeState(state);
            }
        } else if (ItemAbilities.FIRESTARTER_LIGHT == itemAbility) {
            if (CampfireBlock.canLight(state) || CandleBlock.canLight(state) || CandleCakeBlock.canLight(state)) {
                return state.setValue(BlockStateProperties.LIT, true);
            }
        }

        return null;
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
    default void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {}

    /**
     * If this block should be notified of weak changes.
     * Weak changes are changes 1 block away through a solid block.
     * Similar to comparators.
     *
     * @param level The current level
     * @param pos   Block position in level
     * @return true To be notified of changes
     */
    default boolean getWeakChanges(BlockState state, LevelReader level, BlockPos pos) {
        return false;
    }

    /**
     * Sensitive version of getSoundType
     *
     * @param state  The state
     * @param level  The level
     * @param pos    The position. Note that the level may not necessarily have {@code state} here!
     * @param entity The entity that is breaking/stepping on/placing/hitting/falling on this block, or null if no entity is in this context
     * @return A SoundType to use
     */
    default SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return state.getSoundType();
    }
}
