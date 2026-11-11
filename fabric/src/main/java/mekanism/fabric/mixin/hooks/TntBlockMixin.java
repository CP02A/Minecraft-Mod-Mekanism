package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@code IBlockExtension#onCaughtFire} (the additions' obsidian TNT, which extends {@link TntBlock}, spawns its own primed TNT): like NeoForge's
 * patch of {@link TntBlock}, the TNT's ignition sites call {@code onCaughtFire} instead of the static {@link TntBlock#prime}, and {@link TntBlock}'s
 * {@code onCaughtFire} primes it like vanilla.
 */
@Mixin(TntBlock.class)
public abstract class TntBlockMixin {

    private static final String PRIME = "Lnet/minecraft/world/level/block/TntBlock;prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z";
    private static final String PRIME_BY = "Lnet/minecraft/world/level/block/TntBlock;prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Z";

    /**
     * {@code TntBlock}'s override of {@code IBlockExtension#onCaughtFire} (NeoForge's patch adds it to {@link TntBlock}).
     */
    public boolean onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter, ItemStack ignitionItem) {
        return TntBlock.prime(level, pos, igniter, ignitionItem);
    }

    @WrapOperation(method = "onPlace", at = @At(value = "INVOKE", target = PRIME))
    private boolean mekanism$onPlaceCaughtFire(Level level, BlockPos pos, Operation<Boolean> original, BlockState state, Level l, BlockPos p, BlockState oldState,
          boolean movedByPiston) {
        return state.getBlock().onCaughtFire(state, level, pos, null, null, ItemStack.EMPTY);
    }

    @WrapOperation(method = "neighborChanged", at = @At(value = "INVOKE", target = PRIME))
    private boolean mekanism$neighborChangedCaughtFire(Level level, BlockPos pos, Operation<Boolean> original, BlockState state, Level l, BlockPos p, Block block,
          @Nullable Orientation orientation, boolean movedByPiston) {
        return state.getBlock().onCaughtFire(state, level, pos, null, null, ItemStack.EMPTY);
    }

    @WrapOperation(method = "playerWillDestroy", at = @At(value = "INVOKE", target = PRIME))
    private boolean mekanism$playerWillDestroyCaughtFire(Level level, BlockPos pos, Operation<Boolean> original, Level l, BlockPos p, BlockState state, Player player) {
        return state.getBlock().onCaughtFire(state, level, pos, null, null, ItemStack.EMPTY);
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = PRIME_BY))
    private boolean mekanism$useItemOnCaughtFire(Level level, BlockPos pos, @Nullable LivingEntity igniter, ItemStack item, Operation<Boolean> original,
          ItemStack itemStack, BlockState state, Level l, BlockPos p, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return state.getBlock().onCaughtFire(state, level, pos, hitResult.getDirection(), igniter, item);
    }

    @WrapOperation(method = "onProjectileHit", at = @At(value = "INVOKE", target = PRIME_BY))
    private boolean mekanism$projectileCaughtFire(Level level, BlockPos pos, @Nullable LivingEntity igniter, ItemStack item, Operation<Boolean> original,
          Level l, BlockState state, BlockHitResult blockHit, Projectile projectile) {
        return state.getBlock().onCaughtFire(state, level, pos, blockHit.getDirection(), igniter, item);
    }
}
