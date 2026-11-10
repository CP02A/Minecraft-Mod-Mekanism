package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.shim.ShimHooks;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Renders the background set with NeoForge's {@code Slot#setBackground} for empty slots.
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    @ModifyReturnValue(method = "getNoItemIcon", at = @At("RETURN"))
    private @Nullable Identifier mekanism$background(@Nullable Identifier icon) {
        if (icon == null) {
            return ShimHooks.getSlotBackground((Slot) (Object) this);
        }
        return icon;
    }
}
