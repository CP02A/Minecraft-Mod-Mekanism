package mekanism.fabric.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * WTHIT refuses to start on Fabric when {@code net.neoforged.fml.ModList} exists (it assumes it runs on NeoForge), but on Fabric that class is
 * provided by Mekanism's NeoForge shim. Hide the shim's NeoForge classes from that check. Only applied when WTHIT is installed.
 */
@Pseudo
@Mixin(targets = "mcp.mobius.waila.Waila", remap = false)
public class WailaPlatformCheckMixin {

    @Redirect(method = "unsupportedPlatform", at = @At(value = "INVOKE", target = "Ljava/lang/Class;forName(Ljava/lang/String;)Ljava/lang/Class;"), require = 0)
    private static Class<?> mekanism$ignoreNeoForgeShim(String className) throws ClassNotFoundException {
        if (className.startsWith("net.neoforged.")) {
            throw new ClassNotFoundException(className + " is provided by Mekanism's NeoForge shim, not by NeoForge");
        }
        return Class.forName(className);
    }
}
