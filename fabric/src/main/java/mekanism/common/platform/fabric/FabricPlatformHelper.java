package mekanism.common.platform.fabric;

import java.nio.file.Path;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.shim.ShimServices;
import org.jspecify.annotations.Nullable;
import mekanism.common.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Nullable
    @Override
    public ICustomIngredient getCustomIngredient(Ingredient ingredient) {
        return ShimServices.BACKEND.getCustomIngredient(ingredient);
    }

    @Override
    public void setRestoringBlockSnapshots(Level level, boolean restoring) {
        //Not supported: Fabric has no equivalent of NeoForge's block snapshot restoration
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder(int labelColor) {
        return FabricCreativeModeTab.builder();
    }
}
