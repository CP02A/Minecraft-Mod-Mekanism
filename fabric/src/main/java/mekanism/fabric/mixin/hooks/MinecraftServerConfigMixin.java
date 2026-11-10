package mekanism.fabric.mixin.hooks;

import mekanism.fabric.shim.ServerHooks;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Loads the server configs before Fabric applies its biome modifications at the end of the server's constructor (this mixin has a higher priority than Fabric's
 * biome API mixin, so it runs first), as NeoForge's biome modifiers may read server configs (NeoForge applies them after loading server configs). They are
 * unloaded again before Forge Config API Port loads them normally in {@code SERVER_STARTING}.
 */
@Mixin(value = MinecraftServer.class, priority = 900)
public abstract class MinecraftServerConfigMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mekanism$loadServerConfigsEarly(CallbackInfo ci) {
        ServerHooks.loadServerConfigsEarly((MinecraftServer) (Object) this);
    }
}
