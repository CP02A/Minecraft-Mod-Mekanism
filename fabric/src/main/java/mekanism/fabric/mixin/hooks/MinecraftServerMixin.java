package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mekanism.fabric.shim.ServerHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkLoadCounter;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Reinstates the forced chunks of NeoForge's ticket controllers when the levels are initially loaded (after vanilla reinstates its own tickets), like
 * NeoForge's patch of {@code MinecraftServer#prepareLevels}.
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @WrapOperation(method = "prepareLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkLoadCounter;track(Lnet/minecraft/server/level/ServerLevel;Ljava/lang/Runnable;)V"))
    private void mekanism$activateForcedChunks(ChunkLoadCounter counter, ServerLevel level, Runnable scheduler, Operation<Void> original) {
        original.call(counter, level, (Runnable) () -> {
            scheduler.run();
            ServerHooks.activateForcedChunks(level);
        });
    }
}
