package mekanism.fabric.mixin.hooks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's block experience drops ({@code IBlockExtension#getExpDrop}): like NeoForge's {@code CommonHooks#handleBlockDrops} (called by its patches of
 * the {@code Block#dropResources} methods), the experience is processed by the tool's enchantments and dropped after {@code spawnAfterBreak}.
 *
 * @implNote Vanilla blocks keep dropping their experience in {@code spawnAfterBreak} (the shim's {@code getExpDrop} doesn't contain NeoForge's patched in
 * overrides for them, so they return {@code 0} here). NeoForge's {@code BlockDropsEvent} isn't fired.
 */
@Mixin(Block.class)
public abstract class BlockExperienceMixin {

    private static final String SPAWN_AFTER_BREAK = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V";

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
          at = @At(value = "INVOKE", target = SPAWN_AFTER_BREAK, shift = At.Shift.AFTER))
    private static void mekanism$dropExperience(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        mekanism$popExperience(state, level, pos, null, null, ItemStack.EMPTY);
    }

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
          at = @At(value = "INVOKE", target = SPAWN_AFTER_BREAK, shift = At.Shift.AFTER))
    private static void mekanism$dropExperience(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity, CallbackInfo ci) {
        mekanism$popExperience(state, level, pos, blockEntity, null, ItemStack.EMPTY);
    }

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
          at = @At(value = "INVOKE", target = SPAWN_AFTER_BREAK, shift = At.Shift.AFTER))
    private static void mekanism$dropExperience(BlockState state, Level level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker,
          ItemStack tool, CallbackInfo ci) {
        mekanism$popExperience(state, level, pos, blockEntity, breaker, tool);
    }

    @Unique
    private static void mekanism$popExperience(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker,
          ItemStack tool) {
        if (level instanceof ServerLevel serverLevel) {
            int experience = state.getBlock().getExpDrop(state, level, pos, blockEntity, breaker, tool);
            if (experience > 0) {
                experience = EnchantmentHelper.processBlockExperience(serverLevel, tool, experience);
                if (experience > 0) {
                    ((BlockInvoker) state.getBlock()).mekanism$popExperience(serverLevel, pos, experience);
                }
            }
        }
    }
}
