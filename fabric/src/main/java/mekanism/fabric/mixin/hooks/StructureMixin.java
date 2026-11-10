package mekanism.fabric.mixin.hooks;

import java.util.Map;
import mekanism.fabric.shim.BiomeModifierBridge;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Returns the settings modified by NeoForge's structure modifiers (applied by {@link BiomeModifierBridge}), like NeoForge's redirects of the
 * {@code settings} field reads to {@code Structure#getModifiedStructureSettings}.
 */
@Mixin(Structure.class)
public abstract class StructureMixin implements BiomeModifierBridge.ModifiedStructureSettings {

    @Unique
    private Structure.@Nullable StructureSettings mekanism$modifiedSettings;

    @Override
    public void mekanism$setModifiedSettings(Structure.@Nullable StructureSettings settings) {
        this.mekanism$modifiedSettings = settings;
    }

    @Inject(method = "biomes", at = @At("HEAD"), cancellable = true)
    private void mekanism$modifiedBiomes(CallbackInfoReturnable<HolderSet<Biome>> cir) {
        if (mekanism$modifiedSettings != null) {
            cir.setReturnValue(mekanism$modifiedSettings.biomes());
        }
    }

    @Inject(method = "spawnOverrides", at = @At("HEAD"), cancellable = true)
    private void mekanism$modifiedSpawnOverrides(CallbackInfoReturnable<Map<MobCategory, StructureSpawnOverride>> cir) {
        if (mekanism$modifiedSettings != null) {
            cir.setReturnValue(mekanism$modifiedSettings.spawnOverrides());
        }
    }

    @Inject(method = "step", at = @At("HEAD"), cancellable = true)
    private void mekanism$modifiedStep(CallbackInfoReturnable<GenerationStep.Decoration> cir) {
        if (mekanism$modifiedSettings != null) {
            cir.setReturnValue(mekanism$modifiedSettings.step());
        }
    }

    @Inject(method = "terrainAdaptation", at = @At("HEAD"), cancellable = true)
    private void mekanism$modifiedTerrainAdaptation(CallbackInfoReturnable<TerrainAdjustment> cir) {
        if (mekanism$modifiedSettings != null) {
            cir.setReturnValue(mekanism$modifiedSettings.terrainAdaptation());
        }
    }
}
