package mekanism.fabric.shim;

import mekanism.fabric.mixin.hooks.ItemStackInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

/**
 * NeoForge's item hooks for using an item on a block ({@code IItemExtension#onItemUseFirst} and {@code IItemExtension#doesSneakBypassUse}), called from
 * {@code ServerPlayerGameMode#useItemOn} and {@code MultiPlayerGameMode#performUseItemOn} like NeoForge's patches of these methods do.
 */
public final class ItemUseHooks {

    private ItemUseHooks() {
    }

    /**
     * Like NeoForge's {@code ItemStack#onItemUseFirst}: calls {@code IItemExtension#onItemUseFirst} before the block is used.
     */
    public static InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player != null && !player.getAbilities().mayBuild && !stack.canPlaceOnBlockInAdventureMode(new BlockInWorld(context.getLevel(), pos, false))) {
            return InteractionResult.PASS;
        }
        ItemStack stackBeforeUse = stack.copy();
        Item usedItem = stack.getItem();
        InteractionResult result = usedItem.onItemUseFirst(stack, context);
        if (player != null && result instanceof InteractionResult.Success success && success.wasItemInteraction()) {
            player.awardStat(Stats.ITEM_USED.get(usedItem));
            ItemStack transformTo = success.heldItemTransformedTo() == null ? stack : success.heldItemTransformedTo();
            return success.heldItemTransformedTo(((ItemStackInvoker) (Object) transformTo).mekanism$applyAfterUseComponentSideEffects(player, stackBeforeUse));
        }
        return result;
    }

    /**
     * {@return whether using the block should not be suppressed while the player is sneaking with items in their hands} Like NeoForge, this is the case
     * when both held items allow it (empty hands do).
     */
    public static boolean sneakBypassesUse(Player player, LevelReader level, BlockPos pos) {
        return doesSneakBypassUse(player.getMainHandItem(), level, pos, player) && doesSneakBypassUse(player.getOffhandItem(), level, pos, player);
    }

    private static boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return stack.isEmpty() || stack.getItem().doesSneakBypassUse(stack, level, pos, player);
    }
}
