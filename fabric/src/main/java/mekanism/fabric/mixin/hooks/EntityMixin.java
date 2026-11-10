package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mekanism.fabric.shim.ServerHooks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's entity invulnerability check event, and entity tick events for passengers.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyReturnValue(method = "isInvulnerableToBase", at = @At("RETURN"))
    private boolean mekanism$invulnerabilityCheck(boolean isVanillaInvulnerable, DamageSource source) {
        return NeoForge.EVENT_BUS.post(new EntityInvulnerabilityCheckEvent((Entity) (Object) this, source, isVanillaInvulnerable)).isInvulnerable();
    }

    @WrapOperation(method = "rideTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void mekanism$passengerTick(Entity entity, Operation<Void> original) {
        ServerHooks.tickWithEvents(entity, () -> original.call(entity));
    }
}
