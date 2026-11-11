package mekanism.fabric.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Objects;
import mekanism.api.IMekanismAccess;
import mekanism.api.chemical.ChemicalStack;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.recipe_viewer.jei.MekanismJEI;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mekanism.common.registries.MekanismBlocks;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/**
 * Client game test for Mekanism's JEI integration (JEI is only on the client runs' classpath): the plugins are loaded, the chemical ingredient type,
 * recipe categories (with their recipes, including fluid slots), catalysts and item subtypes are registered, and the GUI handlers and recipe transfer
 * handlers apply to a machine's GUI. Also opens JEI's recipe view for Mekanism's machines and takes screenshots.
 */
public class JeiClientTests implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!FabricLoader.getInstance().isModLoaded("jei")) {
            throw new AssertionError("JEI is not loaded in the client game test run");
        }
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            singleplayer.getServer().runCommand("time set day");
            context.waitFor(client -> JeiTestPlugin.runtime != null, 2400);
            context.runOnClient(client -> checkRegistrations(runtime()));

            //Open an enrichment chamber's GUI
            singleplayer.getServer().runCommand("execute as @p at @s run setblock ~2 ~1 ~ mekanism:enrichment_chamber[facing=west]");
            singleplayer.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ -90 20");
            context.waitTicks(20);
            singleplayer.getConnection().waitForChunksRender();
            context.getInput().pressKey(options -> options.keyUse);
            context.waitFor(client -> client.gui.screen() instanceof GuiMekanism<?>);
            context.waitTicks(10);
            context.runOnClient(client -> checkMachineGui(runtime(), (AbstractContainerScreen<?>) client.gui.screen()));
            context.takeScreenshot("jei-machine-gui");

            //Open JEI's recipe view from the machine's GUI (ScreenEvent.Opening marks the GUI as switching to the recipe viewer)
            GuiMekanism<?> machineScreen = context.computeOnClient(client -> (GuiMekanism<?>) client.gui.screen());
            context.runOnClient(client -> runtime().getRecipesGui().showTypes(List.of(MekanismJEI.genericRecipeType(RecipeViewerRecipeType.ENRICHING))));
            context.waitFor(client -> client.gui.screen() instanceof IRecipesGui);
            if (!context.computeOnClient(client -> machineScreen.switchingToRecipeViewer)) {
                throw new AssertionError("The machine GUI was not marked as switching to the recipe viewer (ScreenEvent.Opening)");
            }
            context.waitTicks(10);
            context.takeScreenshot("jei-enriching-recipes");

            //Fluid and chemical slots
            context.runOnClient(client -> runtime().getRecipesGui().showTypes(List.of(MekanismJEI.genericRecipeType(RecipeViewerRecipeType.SEPARATING))));
            context.waitTicks(10);
            context.takeScreenshot("jei-separating-recipes");

            //Recipes of a Mekanism item (the enrichment chamber is a catalyst for its category)
            context.runOnClient(client -> runtime().getRecipesGui().show(runtime().getJeiHelpers().getFocusFactory()
                  .createFocus(RecipeIngredientRole.CRAFTING_STATION, VanillaTypes.ITEM_STACK, new ItemStack(MekanismBlocks.ENRICHMENT_CHAMBER))));
            context.waitTicks(10);
            context.takeScreenshot("jei-enrichment-chamber-uses");

            //Closing JEI returns to the machine's GUI, which is no longer switching
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitFor(client -> client.gui.screen() instanceof GuiMekanism<?>);
            context.waitTicks(5);
            if (context.computeOnClient(client -> client.gui.screen() instanceof GuiMekanism<?> gui && gui.switchingToRecipeViewer)) {
                throw new AssertionError("The machine GUI is still marked as switching to the recipe viewer after returning to it");
            }
            context.setScreen(() -> null);
            context.waitTicks(5);
        }
    }

    private static IJeiRuntime runtime() {
        return Objects.requireNonNull(JeiTestPlugin.runtime, "JEI runtime");
    }

    private static void checkRegistrations(IJeiRuntime runtime) {
        if (MekanismJEI.chemicalStackHelper == null) {
            throw new AssertionError("Mekanism's JEI plugin did not register its ingredients");
        }
        IIngredientManager ingredientManager = runtime.getIngredientManager();
        //Chemical ingredient type
        var chemicalType = IMekanismAccess.INSTANCE.jeiHelper().getChemicalIngredientType();
        if (!ingredientManager.getRegisteredIngredientTypes().contains(chemicalType)) {
            throw new AssertionError("The chemical ingredient type is not registered");
        }
        if (ingredientManager.getAllIngredients(chemicalType).stream().map(ChemicalStack::typeHolder).noneMatch(c -> c.getRegisteredName().equals("mekanism:hydrogen"))) {
            throw new AssertionError("Hydrogen is not a registered chemical ingredient");
        }

        //Recipe categories
        IRecipeManager recipeManager = runtime.getRecipeManager();
        long mekanismCategories = recipeManager.createRecipeCategoryLookup().includeHidden().get()
              .filter(category -> category.getRecipeType().getUid().getNamespace().equals("mekanism"))
              .count();
        if (mekanismCategories < 30) {
            throw new AssertionError("Expected all of Mekanism's recipe categories, found " + mekanismCategories);
        }
        if (recipeManager.createRecipeCategoryLookup().includeHidden().get()
              .noneMatch(category -> category.getRecipeType().getUid().getNamespace().equals("mekanismgenerators"))) {
            throw new AssertionError("Mekanism Generators' JEI plugin did not register its recipe categories");
        }
        IRecipeType<?> enriching = MekanismJEI.genericRecipeType(RecipeViewerRecipeType.ENRICHING);
        if (recipeManager.createRecipeLookup(enriching).get().findAny().isEmpty()) {
            throw new AssertionError("The enrichment chamber category has no recipes");
        }
        //Fluid slots (resolved from NeoForge's fluid slot displays and converted to JEI's Fabric fluid ingredients)
        if (!hasWaterInput(recipeManager, MekanismJEI.genericRecipeType(RecipeViewerRecipeType.SEPARATING))) {
            throw new AssertionError("No electrolytic separator recipe has water as a fluid input");
        }

        //Crafting recipes with Mekanism's serializers (on Fabric only recipes of serializers registered for synchronization reach the client)
        Set<String> craftingRecipes = recipeManager.createRecipeLookup(RecipeTypes.CRAFTING).get()
              .map(recipe -> recipe.id().identifier().toString())
              .collect(Collectors.toSet());
        for (String id : List.of("mekanismgenerators:generator/wind", "mekanism:energy_cube/basic", "mekanismtools:diamond_paxel")) {
            if (!craftingRecipes.contains(id)) {
                throw new AssertionError("JEI doesn't show the crafting recipe " + id);
            }
        }

        //Catalysts
        if (recipeManager.createCraftingStationLookup(enriching).getItemStack().noneMatch(stack -> stack.is(MekanismBlocks.ENRICHMENT_CHAMBER.asItem()))) {
            throw new AssertionError("The enrichment chamber is not a catalyst of its category");
        }

        //Item subtypes: an empty and a full energy cube are different ingredients
        IIngredientHelper<ItemStack> itemHelper = ingredientManager.getIngredientHelper(VanillaTypes.ITEM_STACK);
        long energyCubes = ingredientManager.getAllItemStacks().stream()
              .filter(stack -> stack.is(MekanismBlocks.BASIC_ENERGY_CUBE.asItem()))
              .map(stack -> itemHelper.getUid(stack, UidContext.Ingredient))
              .distinct()
              .count();
        if (energyCubes < 2) {
            throw new AssertionError("Expected the energy cube to have subtypes (empty and charged), found " + energyCubes);
        }
    }

    private static <T> boolean hasWaterInput(IRecipeManager recipeManager, IRecipeType<T> type) {
        IRecipeCategory<T> category = recipeManager.getRecipeCategory(type);
        return recipeManager.createRecipeLookup(type).get().anyMatch(recipe -> recipeManager.getRecipeIngredients(category, recipe).getIngredients(RecipeIngredientRole.INPUT)
              .stream()
              .map(ITypedIngredient::getIngredient)
              .anyMatch(ingredient -> ingredient instanceof IJeiFluidIngredient fluid && fluid.getFluidVariant().getFluid() == Fluids.WATER
                                      && fluid.getAmount() > 0));
    }

    private static void checkMachineGui(IJeiRuntime runtime, AbstractContainerScreen<?> screen) {
        if (runtime.getScreenHelper().getGuiExclusionAreas(screen).findAny().isEmpty()) {
            throw new AssertionError("Mekanism's GUI handler provides no exclusion areas for the machine's tabs");
        }
        if (runtime.getScreenHelper().getGhostIngredientHandlers(screen).isEmpty()) {
            throw new AssertionError("Mekanism's ghost ingredient handler is not registered");
        }
        IRecipeCategory<?> category = runtime.getRecipeManager().getRecipeCategory(MekanismJEI.genericRecipeType(RecipeViewerRecipeType.ENRICHING));
        if (runtime.getRecipeTransferManager().getRecipeTransferHandler(screen.getMenu(), category).isEmpty()) {
            throw new AssertionError("No recipe transfer handler for the enrichment chamber");
        }
    }
}
