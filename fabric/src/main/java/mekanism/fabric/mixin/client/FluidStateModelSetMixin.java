package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@link RegisterFluidModelsEvent} ({@code ClientHooks#gatherFluidModels}) and NeoForge's water tint source.
 */
@Mixin(FluidStateModelSet.class)
public abstract class FluidStateModelSetMixin {

    @ModifyReturnValue(method = "bake", at = @At("RETURN"))
    private static Map<Fluid, FluidModel> mekanism$gatherFluidModels(Map<Fluid, FluidModel> models, MaterialBaker materials) {
        models = new HashMap<>(models);
        //NeoForge's water model uses a fluid tint source with the default water color outside the world (e.g. for fluids in items and GUIs)
        FluidModel water = models.get(Fluids.WATER);
        if (water != null && !(water.tintSource() instanceof FluidTintSource)) {
            FluidModel neoWater = new FluidModel(water.layer(), water.stillMaterial(), water.flowingMaterial(), water.overlayMaterial(), FluidTintSources.water());
            models.replaceAll((fluid, model) -> model == water ? neoWater : model);
        }
        ModLoader.postEvent(new RegisterFluidModelsEvent(models, materials));
        return Map.copyOf(models);
    }
}
