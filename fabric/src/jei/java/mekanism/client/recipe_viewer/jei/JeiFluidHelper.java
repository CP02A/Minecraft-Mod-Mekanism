package mekanism.client.recipe_viewer.jei;

import java.util.List;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.fabric.ingredients.fluids.JeiFluidIngredient;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.display.FluidStackContentsFactory;

/**
 * Loader specific representation of fluids in JEI (the NeoForge version, with the same name, uses {@link FluidStack}s directly). JEI on Fabric represents
 * fluids as {@link IJeiFluidIngredient}s, measured in droplets (81 per millibucket).
 */
public final class JeiFluidHelper {

    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / FluidType.BUCKET_VOLUME;

    private JeiFluidHelper() {
    }

    public static IIngredientTypeWithSubtypes<Fluid, IJeiFluidIngredient> type() {
        return FabricTypes.FLUID_STACK;
    }

    public static IJeiFluidIngredient toJei(FluidStack stack) {
        FluidVariant variant = stack.isEmpty() ? FluidVariant.blank() : FluidVariant.of(stack.getFluid(), stack.getComponentsPatch());
        return new JeiFluidIngredient(variant, toJeiAmount(stack.getAmount()));
    }

    public static FluidStack fromJei(IJeiFluidIngredient ingredient) {
        FluidVariant variant = ingredient.getFluidVariant();
        if (variant.isBlank()) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(variant.getFluid(), (int) Math.min(Integer.MAX_VALUE, ingredient.getAmount() / DROPLETS_PER_MB), variant.getComponentsPatch());
    }

    public static IJeiFluidIngredient withAmount(IJeiFluidIngredient ingredient, int amount) {
        return new JeiFluidIngredient(ingredient.getFluidVariant(), toJeiAmount(amount));
    }

    public static String describe(IJeiFluidIngredient ingredient) {
        return ingredient.getFluidVariant().getFluid().toString();
    }

    /**
     * Converts an ingredient from Mekanism's GUIs (that may be a {@link FluidStack}) to the representation JEI uses.
     */
    public static Object toJeiIngredient(Object ingredient) {
        return ingredient instanceof FluidStack stack ? toJei(stack) : ingredient;
    }

    /**
     * Converts an ingredient from JEI (that may be a JEI fluid ingredient) to the representation Mekanism's GUIs use.
     */
    public static Object fromJeiIngredient(Object ingredient) {
        return ingredient instanceof IJeiFluidIngredient fluid ? fromJei(fluid) : ingredient;
    }

    /**
     * Converts an amount in millibuckets to the amount JEI uses for its fluid ingredients.
     */
    public static long toJeiAmount(int amount) {
        return amount * DROPLETS_PER_MB;
    }

    /**
     * Adds the fluids of the given display to the slot. JEI on Fabric can't resolve NeoForge's fluid slot displays (its fluid helper has no display contents
     * factory), so they are resolved to Mekanism's fluid stacks here and converted.
     */
    public static IRecipeSlotBuilder addDisplay(IRecipeSlotBuilder slotBuilder, SlotDisplay display) {
        List<IJeiFluidIngredient> fluids = display.resolve(slotBuilder.getContextMap(), FluidStackContentsFactory.INSTANCE)
              .filter(stack -> !stack.isEmpty())
              .map(JeiFluidHelper::toJei)
              .toList();
        return slotBuilder.addIngredients(type(), fluids);
    }
}
