package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.Map;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge's {@link ModelEvent.ModifyBakingResult} and {@link ModelEvent.BakingCompleted}. Applied after Fabric's model loading mixin (which stores
 * the extra models, including NeoForge's standalone models, at the end of {@code apply}), so that the standalone models are available to the
 * {@link ModelEvent.BakingCompleted} listeners.
 */
@Mixin(value = ModelManager.class, priority = 1100)
public abstract class ModelManagerMixin {

    /**
     * The model bakery and atlases of the reload that is currently in progress (NeoForge stores the bakery in the model manager).
     */
    @Unique
    private static volatile @Nullable ModelBakery mekanism$modelBakery;
    @Unique
    private static volatile SpriteLoader.@Nullable Preparations mekanism$blockAtlas;
    @Unique
    private static volatile SpriteLoader.@Nullable Preparations mekanism$itemAtlas;

    @Inject(method = "loadModels", at = @At("HEAD"))
    private static void mekanism$captureBakery(CallbackInfoReturnable<?> cir, @Local(argsOnly = true) ModelBakery bakery,
          @Local(argsOnly = true, ordinal = 0) SpriteLoader.Preparations blockAtlas, @Local(argsOnly = true, ordinal = 1) SpriteLoader.Preparations itemAtlas) {
        mekanism$modelBakery = bakery;
        mekanism$blockAtlas = blockAtlas;
        mekanism$itemAtlas = itemAtlas;
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/client/renderer/block/FluidStateModelSet;bake(Lnet/minecraft/client/resources/model/sprite/MaterialBaker;)Ljava/util/Map;"))
    private static Map<Fluid, FluidModel> mekanism$modifyBakingResult(MaterialBaker materialBaker, Operation<Map<Fluid, FluidModel>> original,
          @Local(argsOnly = true) ModelBakery.BakingResult bakingResult) {
        Map<Fluid, FluidModel> fluidModels = original.call(materialBaker);
        ModLoader.postEvent(new ModelEvent.ModifyBakingResult(bakingResult, materialBaker, mekanism$blockAtlas, mekanism$itemAtlas, mekanism$modelBakery));
        return fluidModels;
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void mekanism$bakingCompleted(@Coerce Object reloadState, CallbackInfo ci) {
        ModelBakery.BakingResult bakingResult = ((ModelManagerReloadStateAccessor) reloadState).mekanism$bakedModels();
        ModLoader.postEvent(new ModelEvent.BakingCompleted((ModelManager) (Object) this, bakingResult, mekanism$modelBakery));
    }
}
