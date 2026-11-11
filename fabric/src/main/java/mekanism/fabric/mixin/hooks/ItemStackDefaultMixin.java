package mekanism.fabric.mixin.hooks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Both Fabric API's {@code FabricItemStack} and the shim's {@code ItemInstanceExtension} are injected into {@link ItemStack} with a default
 * {@code getCraftingRemainder()}, which makes calling it throw an {@link IncompatibleClassChangeError} (found by {@code check_injected_interfaces.py}).
 * Implementing it in the class resolves the conflict. Uses Fabric's stack aware {@code FabricItem#getCraftingRemainder(ItemStack)} (which defaults
 * to {@link net.minecraft.world.item.Item#getCraftingRemainder()}, the shim's behavior), like NeoForge's stack aware version.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackDefaultMixin {

    public @Nullable ItemStackTemplate getCraftingRemainder() {
        ItemStack self = (ItemStack) (Object) this;
        return self.getItem().getCraftingRemainder(self);
    }
}
