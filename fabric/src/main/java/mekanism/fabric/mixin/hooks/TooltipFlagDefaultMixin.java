package mekanism.fabric.mixin.hooks;

import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Both Fabric API's {@code FabricTooltipFlag} and the shim's {@code TooltipFlagExtension} are injected into {@link TooltipFlag} with a default
 * {@code shouldDisplayAllInformation} (both {@code false}), which makes calling it on vanilla's flags throw an {@link IncompatibleClassChangeError}.
 * Implementing it in the class resolves the conflict.
 */
@Mixin(TooltipFlag.Default.class)
public abstract class TooltipFlagDefaultMixin {

    public boolean shouldDisplayAllInformation() {
        return false;
    }
}
