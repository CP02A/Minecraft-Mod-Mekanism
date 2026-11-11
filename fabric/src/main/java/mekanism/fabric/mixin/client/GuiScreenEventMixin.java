package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's {@link ScreenEvent.Opening} (which may change or cancel the new screen) and {@link ScreenEvent.Closing}. Unlike NeoForge, opening is fired
 * before vanilla replaces a null screen with the title or death screen, so it is only fired for explicitly opened screens.
 */
@Mixin(Gui.class)
public abstract class GuiScreenEventMixin {

    @Shadow
    @Nullable
    private Screen screen;

    @WrapMethod(method = "setScreen")
    private void mekanism$fireScreenOpening(@Nullable Screen newScreen, Operation<Void> original) {
        if (newScreen != null) {
            ScreenEvent.Opening event = NeoForge.EVENT_BUS.post(new ScreenEvent.Opening(screen, newScreen));
            if (event.isCanceled()) {
                return;
            }
            newScreen = event.getNewScreen();
        }
        original.call(newScreen);
    }

    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"))
    private void mekanism$fireScreenClosing(@Nullable Screen newScreen, CallbackInfo ci) {
        if (screen != null) {
            NeoForge.EVENT_BUS.post(new ScreenEvent.Closing(screen));
        }
    }
}
