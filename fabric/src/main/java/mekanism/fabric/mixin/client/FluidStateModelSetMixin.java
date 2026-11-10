package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@link RegisterFluidModelsEvent} ({@code ClientHooks#gatherFluidModels}).
 */
@Mixin(FluidStateModelSet.class)
public abstract class FluidStateModelSetMixin {

    @ModifyReturnValue(method = "bake", at = @At("RETURN"))
    private static Map<Fluid, FluidModel> mekanism$gatherFluidModels(Map<Fluid, FluidModel> models, MaterialBaker materials) {
        models = new HashMap<>(models);
        ModLoader.postEvent(new RegisterFluidModelsEvent(models, materials));
        return Map.copyOf(models);
    }
}
