/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.world;

import com.mojang.serialization.MapCodec;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.NeoForgeMod;

public final class BiomeModifiers {
    private BiomeModifiers() {} // Utility class.

    /**
     * <p>Stock biome modifier that adds features to biomes. Has the following json format:</p>
     *
     * <pre>
     * {
     *   "type": "neoforge:add_features", // required
     *   "biomes": "#namespace:your_biome_tag" // accepts a biome id, [list of biome ids], or #namespace:biome_tag
     *   "features": "namespace:your_feature", // accepts a placed feature id, [list of placed feature ids], or #namespace:feature_tag
     *   "step": "underground_ores" // accepts a Decoration enum name
     * }
     * </pre>
     *
     * <p>Be wary of using this to add vanilla PlacedFeatures to biomes, as doing so may cause a feature cycle violation.</p>
     *
     * @param biomes   Biomes to add features to.
     * @param features PlacedFeatures to add to biomes.
     * @param step     Decoration step to run features in.
     */
    public record AddFeaturesBiomeModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features,
            Decoration step) implements BiomeModifier {
        @Override
        public void modify(RegistryAccess registries, Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            if (phase == Phase.ADD && this.biomes.contains(biome)) {
                BiomeGenerationSettingsBuilder generationSettings = builder.getGenerationSettings();
                this.features.forEach(holder -> generationSettings.addFeature(this.step, holder));
            }
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec() {
            return NeoForgeMod.ADD_FEATURES_BIOME_MODIFIER_TYPE.get();
        }
    }

    /**
     * <p>Stock biome modifier that removes features from biomes. Has the following json format:</p>
     *
     * <pre>
     * {
     *   "type": "neoforge:remove_features", // required
     *   "biomes": "#namespace:your_biome_tag", // accepts a biome id, [list of biome ids], or #namespace:biome_tag
     *   "features": "namespace:your_feature", // accepts a placed feature id, [list of placed feature ids], or #namespace:feature_tag
     *   "steps": "underground_ores" OR ["underground_ores", "vegetal_decoration"] // one or more decoration steps; optional field, defaults to all steps if not specified
     * }
     * </pre>
     *
     * @param biomes   Biomes to remove features from.
     * @param features PlacedFeatures to remove from biomes.
     * @param steps    Decoration steps to remove features from.
     */
    public record RemoveFeaturesBiomeModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features,
            Set<Decoration> steps) implements BiomeModifier {
        /**
         * Creates a modifier that removes the given features from all decoration steps in the given biomes.
         *
         * @param biomes   Biomes to remove features from.
         * @param features PlacedFeatures to remove from biomes.
         */
        public static RemoveFeaturesBiomeModifier allSteps(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features) {
            return new RemoveFeaturesBiomeModifier(biomes, features, EnumSet.allOf(Decoration.class));
        }

        @Override
        public void modify(RegistryAccess registries, Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            if (phase == Phase.REMOVE && this.biomes.contains(biome)) {
                BiomeGenerationSettingsBuilder generationSettings = builder.getGenerationSettings();
                for (Decoration step : this.steps) {
                    generationSettings.getFeatures(step).removeIf(this.features::contains);
                }
            }
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec() {
            return NeoForgeMod.REMOVE_FEATURES_BIOME_MODIFIER_TYPE.get();
        }
    }






}
