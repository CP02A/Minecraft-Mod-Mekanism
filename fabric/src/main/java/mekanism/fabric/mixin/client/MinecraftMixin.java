package mekanism.fabric.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's client level unload and player logout events.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    @Nullable
    public ClientLevel level;
    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;
    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void mekanism$unloadOnSetLevel(ClientLevel newLevel, CallbackInfo ci) {
        mekanism$unloadLevel();
    }

    @Inject(method = {"disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", "clearClientLevel"},
          at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;", opcode = Opcodes.PUTFIELD))
    private void mekanism$unloadOnDisconnect(CallbackInfo ci) {
        mekanism$unloadLevel();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V",
          at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;gameMode:Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;", opcode = Opcodes.PUTFIELD))
    private void mekanism$logout(CallbackInfo ci) {
        ClientHooks.firePlayerLogout(gameMode, player);
    }

    private void mekanism$unloadLevel() {
        if (level != null) {
            NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
        }
    }
}
