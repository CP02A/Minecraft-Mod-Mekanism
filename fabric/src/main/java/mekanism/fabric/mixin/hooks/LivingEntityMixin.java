package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's living entity damage, fall and jump hooks.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private final Deque<DamageContainer> mekanism$damageContainers = new ArrayDeque<>();

    @Shadow
    public abstract boolean isDeadOrDying();

    @Shadow
    public abstract boolean isInvulnerableTo(ServerLevel level, DamageSource source);

    @Shadow
    public abstract boolean hasEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect);

    @Shadow
    protected abstract float getJumpPower();

    @Unique
    private LivingEntity mekanism$self() {
        return (LivingEntity) (Object) this;
    }

    /**
     * Fires {@link LivingIncomingDamageEvent} once vanilla has checked that the entity can be hurt, and uses the damage container as the source of truth for
     * the damage amount, like NeoForge does.
     */
    @WrapMethod(method = "hurtServer")
    private boolean mekanism$incomingDamage(ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
        if (isInvulnerableTo(level, source) || isDeadOrDying() || source.is(DamageTypeTags.IS_FIRE) && hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return original.call(level, source, damage);
        }
        DamageContainer container = new DamageContainer(source, damage);
        mekanism$damageContainers.push(container);
        try {
            if (NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(mekanism$self(), container)).isCanceled()) {
                return false;
            }
            return original.call(level, source, Math.max(container.getNewDamage(), 0));
        } finally {
            mekanism$damageContainers.pop();
        }
    }

    /**
     * Applies the armor reduction modifiers of the current damage container.
     */
    @ModifyReturnValue(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"))
    private float mekanism$armorReduction(float result, DamageSource source, float damage) {
        DamageContainer container = mekanism$damageContainers.peek();
        if (container == null) {
            return result;
        }
        container.setNewDamage(damage);
        container.setReduction(DamageContainer.Reduction.ARMOR, damage - result);
        return container.getNewDamage();
    }

    @WrapMethod(method = "causeFallDamage")
    private boolean mekanism$fall(double fallDistance, float damageModifier, DamageSource source, Operation<Boolean> original) {
        LivingFallEvent event = NeoForge.EVENT_BUS.post(new LivingFallEvent(mekanism$self(), fallDistance, damageModifier));
        if (event.isCanceled()) {
            return false;
        }
        return original.call(event.getDistance(), event.getDamageMultiplier(), source);
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void mekanism$jump(CallbackInfo ci) {
        if (getJumpPower() > 1.0E-5F) {
            NeoForge.EVENT_BUS.post(new LivingEvent.LivingJumpEvent(mekanism$self()));
        }
    }
}
