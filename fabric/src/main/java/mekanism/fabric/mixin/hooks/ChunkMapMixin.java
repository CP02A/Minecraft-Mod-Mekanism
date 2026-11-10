package mekanism.fabric.mixin.hooks;

import mekanism.fabric.shim.ServerHooks;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's {@link net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent}, fired at the same place as NeoForge (not for chunks that
 * are neither loaded before nor after the change).
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Shadow
    @Final
    ServerLevel level;

    @Inject(method = "updateChunkScheduling", at = @At("TAIL"))
    private void mekanism$ticketLevelUpdated(long node, int level, @Nullable ChunkHolder chunk, int oldLevel, CallbackInfoReturnable<@Nullable ChunkHolder> cir) {
        ServerHooks.fireChunkTicketLevelUpdated(this.level, node, oldLevel, level, cir.getReturnValue());
    }
}
