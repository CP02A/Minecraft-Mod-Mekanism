/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common;

import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.CustomDisplayIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.crafting.DifferenceIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.common.crafting.IntersectionIngredient;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.CompoundFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.CustomDisplayFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.DifferenceFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredientCodecs;
import net.neoforged.neoforge.fluids.crafting.FluidIngredientType;
import net.neoforged.neoforge.fluids.crafting.IntersectionFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SimpleFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.display.FluidSlotDisplay;
import net.neoforged.neoforge.fluids.crafting.display.FluidStackSlotDisplay;
import net.neoforged.neoforge.fluids.crafting.display.FluidTagSlotDisplay;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.holdersets.HolderSetType;
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;

/**
 * Mekanism shim: The subset of NeoForge's own registry entries used by the shared code. Entries are registered immediately when this class is
 * initialized, so {@link #init()} must be called during mod initialization, before registries are frozen.
 */
public class NeoForgeMod {

    public static final String MOD_ID = "neoforge";

    public static final DeferredHolder<SlotDisplay.Type<?>, SlotDisplay.Type<FluidSlotDisplay>> FLUID_SLOT_DISPLAY = register(BuiltInRegistries.SLOT_DISPLAY,
          "fluid", () -> new SlotDisplay.Type<>(FluidSlotDisplay.MAP_CODEC, FluidSlotDisplay.STREAM_CODEC));
    public static final DeferredHolder<SlotDisplay.Type<?>, SlotDisplay.Type<FluidStackSlotDisplay>> FLUID_STACK_SLOT_DISPLAY = register(BuiltInRegistries.SLOT_DISPLAY,
          "fluid_stack", () -> new SlotDisplay.Type<>(FluidStackSlotDisplay.MAP_CODEC, FluidStackSlotDisplay.STREAM_CODEC));
    public static final DeferredHolder<SlotDisplay.Type<?>, SlotDisplay.Type<FluidTagSlotDisplay>> FLUID_TAG_SLOT_DISPLAY = register(BuiltInRegistries.SLOT_DISPLAY,
          "fluid_tag", () -> new SlotDisplay.Type<>(FluidTagSlotDisplay.MAP_CODEC, FluidTagSlotDisplay.STREAM_CODEC));

    public static final DeferredHolder<IngredientType<?>, IngredientType<CompoundIngredient>> COMPOUND_INGREDIENT_TYPE = register(NeoForgeRegistries.INGREDIENT_TYPES,
          "compound", () -> new IngredientType<>(CompoundIngredient.CODEC));
    public static final DeferredHolder<IngredientType<?>, IngredientType<DataComponentIngredient>> DATA_COMPONENT_INGREDIENT_TYPE = register(NeoForgeRegistries.INGREDIENT_TYPES,
          "components", () -> new IngredientType<>(DataComponentIngredient.CODEC));
    public static final DeferredHolder<IngredientType<?>, IngredientType<DifferenceIngredient>> DIFFERENCE_INGREDIENT_TYPE = register(NeoForgeRegistries.INGREDIENT_TYPES,
          "difference", () -> new IngredientType<>(DifferenceIngredient.CODEC));
    public static final DeferredHolder<IngredientType<?>, IngredientType<IntersectionIngredient>> INTERSECTION_INGREDIENT_TYPE = register(NeoForgeRegistries.INGREDIENT_TYPES,
          "intersection", () -> new IngredientType<>(IntersectionIngredient.CODEC));
    public static final DeferredHolder<IngredientType<?>, IngredientType<CustomDisplayIngredient>> CUSTOM_DISPLAY_INGREDIENT = register(NeoForgeRegistries.INGREDIENT_TYPES,
          "custom_display", () -> new IngredientType<>(CustomDisplayIngredient.CODEC));

    public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<SimpleFluidIngredient>> SIMPLE_FLUID_INGREDIENT_TYPE = register(NeoForgeRegistries.FLUID_INGREDIENT_TYPES,
          "simple", FluidIngredientCodecs::simpleType);
    public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<CompoundFluidIngredient>> COMPOUND_FLUID_INGREDIENT_TYPE = register(NeoForgeRegistries.FLUID_INGREDIENT_TYPES,
          "compound", () -> new FluidIngredientType<>(CompoundFluidIngredient.CODEC));
    public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<DataComponentFluidIngredient>> DATA_COMPONENT_FLUID_INGREDIENT_TYPE = register(NeoForgeRegistries.FLUID_INGREDIENT_TYPES,
          "components", () -> new FluidIngredientType<>(DataComponentFluidIngredient.CODEC));
    public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<DifferenceFluidIngredient>> DIFFERENCE_FLUID_INGREDIENT_TYPE = register(NeoForgeRegistries.FLUID_INGREDIENT_TYPES,
          "difference", () -> new FluidIngredientType<>(DifferenceFluidIngredient.CODEC));
    public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<IntersectionFluidIngredient>> INTERSECTION_FLUID_INGREDIENT_TYPE = register(NeoForgeRegistries.FLUID_INGREDIENT_TYPES,
          "intersection", () -> new FluidIngredientType<>(IntersectionFluidIngredient.CODEC));
    public static final DeferredHolder<FluidIngredientType<?>, FluidIngredientType<CustomDisplayFluidIngredient>> CUSTOM_DISPLAY_FLUID_INGREDIENT = register(NeoForgeRegistries.FLUID_INGREDIENT_TYPES,
          "custom_display", () -> new FluidIngredientType<>(CustomDisplayFluidIngredient.CODEC, CustomDisplayFluidIngredient.STREAM_CODEC));

    public static final Holder<HolderSetType> OR_HOLDER_SET = Registry.registerForHolder(NeoForgeRegistries.HOLDER_SET_TYPES,
          Identifier.fromNamespaceAndPath(MOD_ID, "or"), new OrHolderSet.Type());

    public static final Holder<FluidType> EMPTY_TYPE = registerVanillaFluidType("empty", () -> new FluidType(FluidType.Properties.create()
          .descriptionId("block.minecraft.air")
          .motionScale(1D)
          .canPushEntity(false)
          .canSwim(false)
          .canDrown(false)
          .fallDistanceModifier(1F)
          .pathType(null)
          .adjacentPathType(null)
          .density(0)
          .temperature(0)
          .viscosity(0)));
    public static final Holder<FluidType> WATER_TYPE = registerVanillaFluidType("water", () -> new FluidType(FluidType.Properties.create()
          .descriptionId("block.minecraft.water")
          .fallDistanceModifier(0F)
          .canExtinguish(true)
          .canConvertToSource(true)
          .supportsBoating(true)
          .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
          .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
          .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
          .canHydrate(true)
          .isWaterLike(true)));
    public static final Holder<FluidType> LAVA_TYPE = registerVanillaFluidType("lava", () -> new FluidType(FluidType.Properties.create()
          .descriptionId("block.minecraft.lava")
          .canSwim(false)
          .canDrown(false)
          .pathType(PathType.LAVA)
          .adjacentPathType(null)
          .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
          .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
          .lightLevel(15)
          .density(3000)
          .viscosity(6000)
          .temperature(1300)));

    /**
     * Forces the entries of this class to be registered.
     */
    public static void init() {
    }

    private static <R, T extends R> DeferredHolder<R, T> register(Registry<R> registry, String name, Supplier<T> value) {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, name);
        Registry.register(registry, id, value.get());
        return DeferredHolder.create(registry.key(), id);
    }

    private static Holder<FluidType> registerVanillaFluidType(String name, Supplier<FluidType> value) {
        return Registry.registerForHolder(NeoForgeRegistries.FLUID_TYPES, Identifier.withDefaultNamespace(name), value.get());
    }
}
