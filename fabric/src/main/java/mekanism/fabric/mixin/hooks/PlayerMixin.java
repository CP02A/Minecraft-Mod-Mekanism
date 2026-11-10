package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's player tick and break speed events.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void mekanism$tickPre(CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Pre((Player) (Object) this));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void mekanism$tickPost(CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post((Player) (Object) this));
    }

    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
    private float mekanism$breakSpeed(float speed, BlockState state) {
        //Note: Vanilla doesn't pass the position
        PlayerEvent.BreakSpeed event = NeoForge.EVENT_BUS.post(new PlayerEvent.BreakSpeed((Player) (Object) this, state, speed, null));
        return event.isCanceled() ? -1 : event.getNewSpeed();
    }
}
