package mekanism.client.recipe_viewer.jei;

import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Loader specific representation of fluids in JEI (the Fabric version, with the same name, uses JEI's Fabric fluid ingredients). On NeoForge, JEI uses
 * {@link FluidStack}s directly.
 */
public final class JeiFluidHelper {

    private JeiFluidHelper() {
    }

    public static IIngredientTypeWithSubtypes<Fluid, FluidStack> type() {
        return NeoForgeTypes.FLUID_STACK;
    }

    public static FluidStack toJei(FluidStack stack) {
        return stack;
    }

    public static FluidStack withAmount(FluidStack stack, int amount) {
        return stack.copyWithAmount(amount);
    }

    public static String describe(FluidStack stack) {
        return stack.getFluid().toString();
    }

    /**
     * Converts an ingredient from Mekanism's GUIs (that may be a {@link FluidStack}) to the representation JEI uses.
     */
    public static Object toJeiIngredient(Object ingredient) {
        return ingredient;
    }

    /**
     * Converts an ingredient from JEI (that may be a JEI fluid ingredient) to the representation Mekanism's GUIs use.
     */
    public static Object fromJeiIngredient(Object ingredient) {
        return ingredient;
    }

    /**
     * Converts an amount in millibuckets to the amount JEI uses for its fluid ingredients.
     */
    public static long toJeiAmount(int amount) {
        return amount;
    }

    /**
     * Adds the fluids of the given display to the slot.
     */
    public static IRecipeSlotBuilder addDisplay(IRecipeSlotBuilder slotBuilder, SlotDisplay display) {
        return slotBuilder.add(type(), display);
    }
}
