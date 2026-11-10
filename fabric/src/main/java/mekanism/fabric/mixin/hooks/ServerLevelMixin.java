package mekanism.fabric.mixin.hooks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's entity tick events for entities that aren't passengers (on the server).
 * <p>
 * Note: Unlike on NeoForge, cancelling the pre event doesn't prevent the entity from ticking.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void mekanism$entityTickPre(Entity entity, CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Pre(entity));
    }

    @Inject(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V", shift = At.Shift.AFTER))
    private void mekanism$entityTickPost(Entity entity, CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(entity));
    }
}
