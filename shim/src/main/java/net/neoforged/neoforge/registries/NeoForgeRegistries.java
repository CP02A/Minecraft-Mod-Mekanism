/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.registries;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.FluidIngredientType;
import net.neoforged.neoforge.registries.holdersets.HolderSetType;
import net.neoforged.neoforge.shim.ShimServices;

/**
 * Mekanism shim: The subset of NeoForge's registries used by the shared code, created through the loader's registry system.
 */
public class NeoForgeRegistries {

    public static final Registry<FluidType> FLUID_TYPES = ShimServices.BACKEND.createRegistry(Keys.FLUID_TYPES, true);
    public static final Registry<IngredientType<?>> INGREDIENT_TYPES = ShimServices.BACKEND.createRegistry(Keys.INGREDIENT_TYPES, true);
    public static final Registry<FluidIngredientType<?>> FLUID_INGREDIENT_TYPES = ShimServices.BACKEND.createRegistry(Keys.FLUID_INGREDIENT_TYPES, true);
    public static final Registry<HolderSetType> HOLDER_SET_TYPES = ShimServices.BACKEND.createRegistry(Keys.HOLDER_SET_TYPES, true);
    public static final Registry<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS = ShimServices.BACKEND.createRegistry(Keys.BIOME_MODIFIER_SERIALIZERS, false);
    public static final Registry<MapCodec<? extends StructureModifier>> STRUCTURE_MODIFIER_SERIALIZERS = ShimServices.BACKEND.createRegistry(Keys.STRUCTURE_MODIFIER_SERIALIZERS, false);
    public static final Registry<MapCodec<? extends ICondition>> CONDITION_SERIALIZERS = ShimServices.BACKEND.createRegistry(Keys.CONDITION_CODECS, false);

    public static final class Keys {

        public static final ResourceKey<Registry<FluidType>> FLUID_TYPES = key("fluid_type");
        public static final ResourceKey<Registry<IngredientType<?>>> INGREDIENT_TYPES = key("ingredient_serializer");
        public static final ResourceKey<Registry<FluidIngredientType<?>>> FLUID_INGREDIENT_TYPES = key("fluid_ingredient_type");
        public static final ResourceKey<Registry<HolderSetType>> HOLDER_SET_TYPES = key("holder_set_type");
        public static final ResourceKey<Registry<MapCodec<? extends BiomeModifier>>> BIOME_MODIFIER_SERIALIZERS = key("biome_modifier_serializers");
        public static final ResourceKey<Registry<MapCodec<? extends StructureModifier>>> STRUCTURE_MODIFIER_SERIALIZERS = key("structure_modifier_serializers");
        public static final ResourceKey<Registry<MapCodec<? extends ICondition>>> CONDITION_CODECS = key("condition_codecs");
        // Datapack registries
        public static final ResourceKey<Registry<BiomeModifier>> BIOME_MODIFIERS = key("biome_modifier");
        public static final ResourceKey<Registry<StructureModifier>> STRUCTURE_MODIFIERS = key("structure_modifier");

        private static <T> ResourceKey<Registry<T>> key(String name) {
            return ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath("neoforge", name));
        }
    }
}
