/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.event.entity.living;

import com.mojang.datafixers.util.Either;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * This event is fired before {@link Mob#finalizeSpawn} is called.<br>
 * This allows mods to control mob initialization.
 * <p>
 * Canceling this event will result in {@link Mob#finalizeSpawn} not being called, and the returned value always being null, instead of propagating the SpawnGroupData.<br>
 * The entity will still be spawned.
 * <p>
 * This event is fired on {@link NeoForge#EVENT_BUS}, and is only fired on the logical server.
 * <p>
 * Mekanism shim: Fired at the start of {@link Mob#finalizeSpawn} (instead of before its call sites), without a spawner. The spawn cancellation status is
 * stored in the event (instead of the mob) and applied to the mob by the loader after the event was posted.
 */
public class FinalizeSpawnEvent extends MobSpawnEvent implements ICancellableEvent {
    private final EntitySpawnReason spawnType;

    @Nullable
    private final Either<BlockEntity, Entity> spawner;

    private DifficultyInstance difficulty;

    //Mekanism shim: NeoForge stores this in the mob
    private boolean spawnCancelled;

    @Nullable
    private SpawnGroupData spawnData;

    @ApiStatus.Internal
    public FinalizeSpawnEvent(Mob entity, ServerLevelAccessor level, double x, double y, double z, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData spawnData, @Nullable Either<BlockEntity, Entity> spawner) {
        super(entity, level, x, y, z);
        this.difficulty = difficulty;
        this.spawnType = spawnType;
        this.spawnData = spawnData;
        this.spawner = spawner;
    }

    /**
     * Retrieves the {@link DifficultyInstance} for the chunk where the mob is about to be spawned.
     *
     * @return The local difficulty instance
     */
    public DifficultyInstance getDifficulty() {
        return this.difficulty;
    }

    /**
     * Sets the difficulty instance for this event, which will be propagated to {@link Mob#finalizeSpawn} unless cancelled.
     *
     * @param inst The new difficulty instance.
     */
    public void setDifficulty(DifficultyInstance inst) {
        this.difficulty = inst;
    }

    /**
     * Retrieves the type of mob spawn that happened (the event that caused the spawn).
     *
     * @return The mob spawn type.
     */
    public EntitySpawnReason getSpawnType() {
        return this.spawnType;
    }

    /**
     * Retrieves the {@link SpawnGroupData} for this entity.
     *
     * @return The spawn group data.
     */
    @Nullable
    public SpawnGroupData getSpawnData() {
        return this.spawnData;
    }

    /**
     * Sets the spawn data for this entity. If this event is cancelled, this value is not used, since {@link Mob#finalizeSpawn} will not be called.
     *
     * @param data The new spawn data
     */
    public void setSpawnData(@Nullable SpawnGroupData data) {
        this.spawnData = data;
    }

    /**
     * Retrieves the underlying {@link BlockEntity} or {@link Entity} that performed the spawn.
     *
     * @return The spawner responsible for triggering the spawn, or null if none is available.
     */
    @Nullable
    public Either<BlockEntity, Entity> getSpawner() {
        return this.spawner;
    }

    /**
     * This method can be used to cancel the spawn of this mob.<p>
     * This method must be used if you want to block the spawn, as canceling the event only blocks the call to {@link Mob#finalizeSpawn}.<p>
     * Note that if the spawn is cancelled, but the event is not, then {@link Mob#finalizeSpawn} will still be called, but the entity will not be spawned.
     * Usually that has no side effects, but callers should be aware.
     *
     * @param cancel If the spawn should be cancelled (or not).
     */
    public void setSpawnCancelled(boolean cancel) {
        this.spawnCancelled = cancel;
    }

    /**
     * Returns the current spawn cancellation status, which can be changed via {@link FinalizeSpawnEvent#setSpawnCancelled(boolean)}.
     *
     * @return If this mob's spawn is cancelled or not.
     */
    public boolean isSpawnCancelled() {
        return this.spawnCancelled;
    }
}
