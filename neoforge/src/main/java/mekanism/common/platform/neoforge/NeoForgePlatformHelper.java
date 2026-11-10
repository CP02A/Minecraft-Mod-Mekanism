package mekanism.common.platform.neoforge;

import java.nio.file.Path;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import org.jspecify.annotations.Nullable;
import mekanism.common.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.isProduction();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Nullable
    @Override
    public ICustomIngredient getCustomIngredient(Ingredient ingredient) {
        return ingredient.getCustomIngredient();
    }

    @Override
    public void setRestoringBlockSnapshots(Level level, boolean restoring) {
        level.restoringBlockSnapshots = restoring;
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder(int labelColor) {
        return CreativeModeTab.builder().withTabFactory(builder -> new MekanismCreativeTab(builder, labelColor));
    }

    private static class MekanismCreativeTab extends CreativeModeTab {

        private final int labelColor;

        private MekanismCreativeTab(CreativeModeTab.Builder builder, int labelColor) {
            super(builder);
            this.labelColor = labelColor;
        }

        @Override
        public int getLabelColor() {
            return labelColor;
        }
    }
}
