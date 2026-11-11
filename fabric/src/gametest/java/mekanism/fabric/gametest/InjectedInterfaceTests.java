package mekanism.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.fabricmc.fabric.api.item.v1.FabricTooltipFlag;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.extensions.ItemInstanceExtension;
import net.neoforged.neoforge.common.extensions.TooltipFlagExtension;

/**
 * Calls the methods that both a shim and a Fabric API injected interface declare a default for (found by {@code check_injected_interfaces.py}), which
 * throw an {@link IncompatibleClassChangeError} unless a mixin implements them on the class. Each is called through the class and both interfaces.
 */
public class InjectedInterfaceTests {

    @GameTest
    public void tooltipFlagShouldDisplayAllInformation(GameTestHelper helper) {
        TooltipFlag flag = TooltipFlag.Default.ADVANCED;
        helper.assertFalse(flag.shouldDisplayAllInformation(), Component.literal("TooltipFlag#shouldDisplayAllInformation"));
        helper.assertFalse(((TooltipFlagExtension) flag).shouldDisplayAllInformation(), Component.literal("TooltipFlagExtension#shouldDisplayAllInformation"));
        helper.assertFalse(((FabricTooltipFlag) flag).shouldDisplayAllInformation(), Component.literal("FabricTooltipFlag#shouldDisplayAllInformation"));
        helper.succeed();
    }

    @GameTest
    public void itemStackGetCraftingRemainder(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.LAVA_BUCKET);
        checkRemainder(helper, stack.getCraftingRemainder(), "ItemStack#getCraftingRemainder");
        checkRemainder(helper, ((ItemInstanceExtension) stack).getCraftingRemainder(), "ItemInstanceExtension#getCraftingRemainder");
        checkRemainder(helper, ((FabricItemStack) stack).getCraftingRemainder(), "FabricItemStack#getCraftingRemainder");
        helper.assertTrue(new ItemStack(Items.STONE).getCraftingRemainder() == null, Component.literal("Stone has no crafting remainder"));
        helper.succeed();
    }

    private static void checkRemainder(GameTestHelper helper, ItemStackTemplate remainder, String method) {
        helper.assertTrue(remainder != null && remainder.item().value() == Items.BUCKET, Component.literal(method + " of a lava bucket is " + remainder));
    }
}
