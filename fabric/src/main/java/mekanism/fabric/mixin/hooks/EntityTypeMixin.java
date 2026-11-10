package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mekanism.fabric.shim.ServerHooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Discards mobs whose spawn was cancelled by NeoForge's {@code FinalizeSpawnEvent} when they are created to be spawned, like NeoForge's patch (which also
 * skips the post spawn processor).
 */
@Mixin(EntityType.class)
public abstract class EntityTypeMixin {

    @ModifyReturnValue(method = "create(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PostSpawnProcessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;",
          at = @At("RETURN"))
    private @Nullable Entity mekanism$spawnCancelled(@Nullable Entity entity) {
        if (entity != null && ServerHooks.isSpawnCancelled(entity)) {
            entity.discard();
            return null;
        }
        return entity;
    }
}
