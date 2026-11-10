package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.ScrollWheelHandler;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's mouse scrolling event (fired when scrolling outside of screens). Cancelling it skips the vanilla handling by reporting no scroll.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;onMouseScroll(DD)Lorg/joml/Vector2i;"))
    private Vector2i mekanism$scroll(ScrollWheelHandler handler, double scrollX, double scrollY, Operation<Vector2i> original) {
        Vector2i wheelXY = original.call(handler, scrollX, scrollY);
        if ((wheelXY.x != 0 || wheelXY.y != 0) && ClientHooks.onMouseScroll((MouseHandler) (Object) this, scrollX, scrollY, wheelXY)) {
            return new Vector2i();
        }
        return wheelXY;
    }
}
