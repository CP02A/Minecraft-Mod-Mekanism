package mekanism.fabric.mixin.client;

import net.minecraft.client.resources.model.ModelBakery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.resources.model.ModelManager$ReloadState")
public interface ModelManagerReloadStateAccessor {

    @Accessor("bakedModels")
    ModelBakery.BakingResult mekanism$bakedModels();
}
