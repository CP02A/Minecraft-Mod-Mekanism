package mekanism.common.platform.services;

import java.nio.file.Path;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import org.jspecify.annotations.Nullable;

/**
 * Exposes basic information about the mod loader Mekanism is currently running on.
 *
 * @implNote Each loader specific project provides an implementation of this via a {@link java.util.ServiceLoader} service file.
 */
public interface IPlatformHelper {

    /**
     * {@return the name of the mod loader Mekanism is running on}
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given mod id is loaded.
     *
     * @param modId Mod id to check.
     *
     * @return {@code true} if the mod is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * {@return {@code true} if Mekanism is running in a development environment, {@code false} if it is running in production}
     */
    boolean isDevelopmentEnvironment();

    /**
     * {@return the directory that configs should be stored in}
     */
    Path getConfigDirectory();

    /**
     * {@return the NeoForge style custom ingredient backing the given ingredient, or {@code null} if it is not a custom ingredient}
     *
     * @implNote Fabric has its own method of the same name, so this can't be called directly on the ingredient.
     */
    @Nullable
    ICustomIngredient getCustomIngredient(Ingredient ingredient);

    /**
     * Marks the level as restoring block snapshots, which suppresses item drops of blocks that are replaced.
     *
     * @implNote Only supported on NeoForge.
     */
    void setRestoringBlockSnapshots(Level level, boolean restoring);

    /**
     * {@return a new builder for a standalone creative tab}
     *
     * @param labelColor Color of the tab's label. Only supported on NeoForge.
     */
    CreativeModeTab.Builder creativeTabBuilder(int labelColor);
}
