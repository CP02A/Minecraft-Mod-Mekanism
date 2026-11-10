package mekanism.fabric.mixin.hooks;

import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Bridges NeoForge's item extension hooks to the equivalent Fabric API ones: {@code IItemExtension#shouldCauseReequipAnimation} (Fabric only asks when the
 * components or count of the held stack changed, vanilla always plays the animation when the item changed) and
 * {@code IItemExtension#shouldCauseBlockBreakReset}.
 */
@Mixin(Item.class)
public abstract class ItemMixin implements FabricItem {

    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        //Note: The selected slot didn't change, as Fabric only calls this when the item in the hand stayed the same
        return ((Item) (Object) this).shouldCauseReequipAnimation(oldStack, newStack, false);
    }

    @Override
    public boolean allowContinuingBlockBreaking(Player player, ItemStack oldStack, ItemStack newStack) {
        return !((Item) (Object) this).shouldCauseBlockBreakReset(oldStack, newStack);
    }
}
