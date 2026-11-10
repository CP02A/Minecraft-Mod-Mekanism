package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.searchtree.FullTextSearchTree;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.RecipeBookManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Indexes the recipe book search with the result indexers registered via NeoForge's {@code RegisterRecipeBookSearchCategoriesEvent} (which include
 * vanilla's item results), so that modded results (such as chemicals) can be searched.
 */
@Mixin(SessionSearchTrees.class)
public abstract class SessionSearchTreesMixin {

    @WrapOperation(method = "lambda$updateRecipes$1", at = @At(value = "NEW", target = "Lnet/minecraft/client/searchtree/FullTextSearchTree;"))
    private static FullTextSearchTree<RecipeCollection> mekanism$indexModdedResults(Function<RecipeCollection, Stream<String>> tooltipLines,
          Function<RecipeCollection, Stream<Identifier>> resultIds, List<RecipeCollection> recipes, Operation<FullTextSearchTree<RecipeCollection>> original,
          @Local(argsOnly = true) ContextMap recipeContext, @Local(argsOnly = true) Item.TooltipContext tooltipContext,
          @Local(argsOnly = true) TooltipFlag tooltipFlag, @Local(argsOnly = true) Registry<Item> itemRegistry) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return original.call(tooltipLines, resultIds, recipes);
        }
        RegistryAccess registryAccess = connection.registryAccess();
        return original.call(
              (Function<RecipeCollection, Stream<String>>) collection -> RecipeBookManager.getTooltipLines(collection, recipeContext, tooltipContext, tooltipFlag),
              (Function<RecipeCollection, Stream<Identifier>>) collection -> collection.getRecipes().stream()
                    .flatMap(entry -> RecipeBookManager.getResultIds(entry, registryAccess, itemRegistry, recipeContext)),
              recipes
        );
    }
}
