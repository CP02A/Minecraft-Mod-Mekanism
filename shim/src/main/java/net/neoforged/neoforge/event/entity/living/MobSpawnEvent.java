/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.event.entity.EntityEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * This class holds all events relating to the entire flow of mob spawns.
 * <p>
 * Mekanism shim: Only the base class (for {@link FinalizeSpawnEvent}); the spawn placement and position checks are not included.
 */
public abstract class MobSpawnEvent extends EntityEvent {
    private final ServerLevelAccessor level;
    private final double x;
    private final double y;
    private final double z;

    @ApiStatus.Internal
    protected MobSpawnEvent(Mob mob, ServerLevelAccessor level, double x, double y, double z) {
        super(mob);
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Mob getEntity() {
        return (Mob) super.getEntity();
    }

    /**
     * @return The level relating to the mob spawn action
     */
    public ServerLevelAccessor getLevel() {
        return this.level;
    }

    /**
     * @return The x-coordinate relating to the mob spawn action
     */
    public double getX() {
        return this.x;
    }

    /**
     * @return The y-coordinate relating to the mob spawn action
     */
    public double getY() {
        return this.y;
    }

    /**
     * @return The z-coordinate relating to the mob spawn action
     */
    public double getZ() {
        return this.z;
    }
}
