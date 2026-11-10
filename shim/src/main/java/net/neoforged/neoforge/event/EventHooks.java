/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.event;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's event hooks used by the shared code.
 */
public class EventHooks {

    public static BlockEvent.NeighborNotifyEvent onNeighborNotify(Level level, BlockPos pos, BlockState state, EnumSet<Direction> notifiedSides,
          boolean forceRedstoneUpdate) {
        BlockEvent.NeighborNotifyEvent event = new BlockEvent.NeighborNotifyEvent(level, pos, state, notifiedSides, forceRedstoneUpdate);
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    public static boolean onBlockPlace(@Nullable Entity entity, BlockSnapshot blockSnapshot, Direction direction) {
        BlockState placedAgainst = blockSnapshot.getLevel().getBlockState(blockSnapshot.getPos().relative(direction.getOpposite()));
        BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(blockSnapshot, placedAgainst, entity);
        return NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    public static boolean onProjectileImpact(Projectile projectile, HitResult ray) {
        return NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(projectile, ray)).isCanceled();
    }

    public static boolean onExplosionStart(Level level, ServerExplosion explosion) {
        return NeoForge.EVENT_BUS.post(new ExplosionEvent.Start(level, explosion)).isCanceled();
    }

    public static boolean onEntityStruckByLightning(Entity entity, LightningBolt bolt) {
        return NeoForge.EVENT_BUS.post(new EntityStruckByLightningEvent(entity, bolt)).isCanceled();
    }
}
