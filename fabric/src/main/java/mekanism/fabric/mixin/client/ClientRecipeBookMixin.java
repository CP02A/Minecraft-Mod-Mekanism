package mekanism.fabric.mixin.client;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.neoforged.neoforge.client.RecipeBookManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Fills the modded search categories registered via NeoForge's {@code RegisterRecipeBookSearchCategoriesEvent} like vanilla's search categories.
 */
@Mixin(ClientRecipeBook.class)
public abstract class ClientRecipeBookMixin {

    @ModifyArg(method = "rebuildCollections", at = @At(value = "INVOKE", target = "Ljava/util/Map;copyOf(Ljava/util/Map;)Ljava/util/Map;"))
    private Map<ExtendedRecipeBookCategory, List<RecipeCollection>> mekanism$addModdedSearchCategories(Map<ExtendedRecipeBookCategory, List<RecipeCollection>> byCategory) {
        for (var entry : RecipeBookManager.getSearchCategories().entrySet()) {
            byCategory.put(entry.getKey(), entry.getValue().stream()
                  .flatMap(category -> byCategory.getOrDefault(category, List.of()).stream())
                  .collect(ImmutableList.toImmutableList()));
        }
        return byCategory;
    }
}
