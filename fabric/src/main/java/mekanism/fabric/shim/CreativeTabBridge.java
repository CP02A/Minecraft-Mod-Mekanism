package mekanism.fabric.shim;

import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.common.util.InsertableLinkedOpenCustomHashSet;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Fires NeoForge's {@link BuildCreativeModeTabContentsEvent} for every creative tab (through Fabric's creative tab output event), like NeoForge's
 * {@code EventHooks#onCreativeModeTabBuildContents}, and applies the resulting entries.
 */
public final class CreativeTabBridge {

    private CreativeTabBridge() {
    }

    public static void register() {
        CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register(CreativeTabBridge::modifyOutput);
    }

    private static void modifyOutput(CreativeModeTab tab, FabricCreativeModeTabOutput output) {
        Optional<ResourceKey<CreativeModeTab>> tabKey = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab);
        if (tabKey.isEmpty()) {
            return;
        }
        InsertableLinkedOpenCustomHashSet<ItemStack> parentEntries = new InsertableLinkedOpenCustomHashSet<>(ItemStackLinkedSet.TYPE_AND_TAG);
        InsertableLinkedOpenCustomHashSet<ItemStack> searchEntries = new InsertableLinkedOpenCustomHashSet<>(ItemStackLinkedSet.TYPE_AND_TAG);
        List<ItemStack> displayStacks = output.getDisplayStacks();
        List<ItemStack> searchStacks = output.getSearchTabStacks();
        parentEntries.addAll(displayStacks);
        searchEntries.addAll(searchStacks);
        ModLoader.postEvent(new BuildCreativeModeTabContentsEvent(tab, tabKey.get(), output.getContext(), parentEntries, searchEntries));
        displayStacks.clear();
        displayStacks.addAll(parentEntries);
        searchStacks.clear();
        searchStacks.addAll(searchEntries);
    }
}
