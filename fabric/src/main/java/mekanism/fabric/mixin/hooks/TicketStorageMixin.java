package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import mekanism.fabric.shim.ServerHooks;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Persistence of the forced chunks of NeoForge's ticket controllers (which the shim tracks per ticket storage), like NeoForge's patches of
 * {@link TicketStorage}.
 */
@Mixin(TicketStorage.class)
public abstract class TicketStorageMixin {

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"))
    private static Codec<TicketStorage> mekanism$extendCodec(Codec<TicketStorage> codec) {
        return ServerHooks.extendTicketStorageCodec(codec);
    }

    @Inject(method = "deactivateTicketsOnClosing", at = @At("TAIL"))
    private void mekanism$deactivateForcedChunks(CallbackInfo ci) {
        TicketStorage storage = (TicketStorage) (Object) this;
        storage.getBlockForcedChunks().deactivateTicketsOnClosing();
        storage.getEntityForcedChunks().deactivateTicketsOnClosing();
    }
}
