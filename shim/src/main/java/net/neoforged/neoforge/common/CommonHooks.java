/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's common hooks used by the shared code.
 */
public class CommonHooks {

    private static final ThreadLocal<@Nullable Player> craftingPlayer = new ThreadLocal<>();

    public static void setCraftingPlayer(@Nullable Player player) {
        craftingPlayer.set(player);
    }

    @Nullable
    public static Player getCraftingPlayer() {
        return craftingPlayer.get();
    }

    public static BreakBlockEvent fireBlockBreak(Level level, GameType gameType, Player player, BlockPos pos, BlockState state) {
        boolean preCancelEvent = false;
        ItemStack itemstack = player.getMainHandItem();
        if (!itemstack.isEmpty() && !itemstack.canDestroyBlock(state, level, pos, player)) {
            preCancelEvent = true;
        }
        if (player.blockActionRestricted(level, pos, gameType)) {
            preCancelEvent = true;
        }
        if (state.getBlock() instanceof GameMasterBlock && !player.canUseGameMasterBlocks()) {
            preCancelEvent = true;
        }
        var event = new BreakBlockEvent(level, pos, state, player);
        event.setCanceled(preCancelEvent);
        NeoForge.EVENT_BUS.post(event);
        // If the event is canceled on the server, let the client know the block still exists
        if (event.isCanceled() && event.shouldNotifyClient() && player instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundBlockUpdatePacket(pos, state));
        }
        return event;
    }

    /**
     * Mekanism shim: NeoForge's tool modification of blocks via shears in dispensers is not supported.
     */
    public static boolean tryDispenseShearsHarvestBlock(BlockSource source, ItemStack stack, ServerLevel level, BlockPos pos) {
        return false;
    }

    public static LivingShieldBlockEvent onDamageBlock(LivingEntity blocker, DamageContainer container, float blockedDamage, boolean originalBlocked) {
        LivingShieldBlockEvent e = new LivingShieldBlockEvent(blocker, container, blockedDamage, originalBlocked);
        NeoForge.EVENT_BUS.post(e);
        return e;
    }
}
