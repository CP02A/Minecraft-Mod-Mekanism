package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mekanism.fabric.shim.ServerHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's entity tick events for entities that aren't passengers (on the server; passengers are handled by {@link EntityMixin}), and keeping
 * levels with forced chunks of ticket controllers active, and not adding mobs whose spawn was cancelled by NeoForge's {@code FinalizeSpawnEvent} (NeoForge
 * cancels their {@code EntityJoinLevelEvent}).
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @WrapOperation(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void mekanism$entityTick(Entity entity, Operation<Void> original) {
        ServerHooks.tickWithEvents(entity, () -> original.call(entity));
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;hasActiveTickets()Z"))
    private boolean mekanism$hasForcedChunks(boolean hasActiveTickets) {
        return hasActiveTickets || ForcedChunkManager.hasForcedChunks((ServerLevel) (Object) this);
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void mekanism$spawnCancelled(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ServerHooks.isSpawnCancelled(entity)) {
            cir.setReturnValue(false);
        }
    }
}
