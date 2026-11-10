/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common;

import com.mojang.datafixers.util.Either;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.material.FluidState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jspecify.annotations.Nullable;
import java.util.function.Supplier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.common.BooleanAttribute;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.common.conditions.*;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.common.world.BiomeModifiers.*;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.common.world.StructureModifiers;
import net.neoforged.neoforge.registries.holdersets.AnyHolderSet;
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
import net.neoforged.neoforge.common.world.NoneStructureModifier;
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

    // Mekanism shim: Registered so that the structure modifier serializer registry isn't empty
    public static final DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<NoneStructureModifier>> NONE_STRUCTURE_MODIFIER_TYPE = register(NeoForgeRegistries.STRUCTURE_MODIFIER_SERIALIZERS,
          "none", () -> NoneStructureModifier.CODEC);

    public static final Holder<HolderSetType> OR_HOLDER_SET = Registry.registerForHolder(NeoForgeRegistries.HOLDER_SET_TYPES,
          Identifier.fromNamespaceAndPath(MOD_ID, "or"), new OrHolderSet.Type());

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<AddFeaturesBiomeModifier>> ADD_FEATURES_BIOME_MODIFIER_TYPE = register(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, "add_features", () -> RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(
                            Biome.LIST_CODEC.fieldOf("biomes").forGetter(AddFeaturesBiomeModifier::biomes),
                            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(AddFeaturesBiomeModifier::features),
                            Decoration.CODEC.fieldOf("step").forGetter(AddFeaturesBiomeModifier::step))
                    .apply(builder, AddFeaturesBiomeModifier::new)));

    /**
     * Stock biome modifier for removing features from biomes.
     */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<RemoveFeaturesBiomeModifier>> REMOVE_FEATURES_BIOME_MODIFIER_TYPE = register(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, "remove_features", () -> RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(
                            Biome.LIST_CODEC.fieldOf("biomes").forGetter(RemoveFeaturesBiomeModifier::biomes),
                            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(RemoveFeaturesBiomeModifier::features),
                            Codec.<List<Decoration>, Decoration>either(Decoration.CODEC.listOf(), Decoration.CODEC).<Set<Decoration>>xmap(
                                    either -> either.map(Set::copyOf, Set::of), // convert list/singleton to set when decoding
                                    set -> set.size() == 1 ? Either.right(set.toArray(Decoration[]::new)[0]) : Either.left(List.copyOf(set))).optionalFieldOf("steps", EnumSet.allOf(Decoration.class)).forGetter(RemoveFeaturesBiomeModifier::steps))
                    .apply(builder, RemoveFeaturesBiomeModifier::new)));

    /**
     * Stock biome modifier for adding mob spawns to biomes.
     */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<AddSpawnsBiomeModifier>> ADD_SPAWNS_BIOME_MODIFIER_TYPE = register(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, "add_spawns", () -> RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(
                            Biome.LIST_CODEC.fieldOf("biomes").forGetter(AddSpawnsBiomeModifier::biomes),
                            // Allow either a list or single spawner, attempting to decode the list format first.
                            // Uses the better EitherCodec that logs both errors if both formats fail to parse.
                            Codec.either(WeightedList.codec(SpawnerData.CODEC), Weighted.codec(SpawnerData.CODEC)).xmap(
                                    either -> either.map(Function.identity(), WeightedList::<SpawnerData>of), // convert list/singleton to list when decoding
                                    list -> list.unwrap().size() == 1 ? Either.right(list.unwrap().get(0)) : Either.left(list) // convert list to singleton/list when encoding
                            ).fieldOf("spawners").forGetter(AddSpawnsBiomeModifier::spawners))
                    .apply(builder, AddSpawnsBiomeModifier::new)));

    /**
     * Stock biome modifier for removing mob spawns from biomes.
     */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<RemoveSpawnsBiomeModifier>> REMOVE_SPAWNS_BIOME_MODIFIER_TYPE = register(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, "remove_spawns", () -> RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(
                            Biome.LIST_CODEC.fieldOf("biomes").forGetter(RemoveSpawnsBiomeModifier::biomes),
                            RegistryCodecs.holderSet(Registries.ENTITY_TYPE).fieldOf("entity_types").forGetter(RemoveSpawnsBiomeModifier::entityTypes))
                    .apply(builder, RemoveSpawnsBiomeModifier::new)));

    /**
     * Stock biome modifier for adding carvers to biomes.
     */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.AddCarversBiomeModifier>> ADD_CARVERS_BIOME_MODIFIER_TYPE = register(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, "add_carvers", () -> RecordCodecBuilder.mapCodec(builder -> builder.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.AddCarversBiomeModifier::biomes),
            WorldCarver.LIST_CODEC.fieldOf("carvers").forGetter(BiomeModifiers.AddCarversBiomeModifier::carvers)).apply(builder, BiomeModifiers.AddCarversBiomeModifier::new)));

    /**
     * Stock biome modifier for removing carvers from biomes.
     */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.RemoveCarversBiomeModifier>> REMOVE_CARVERS_BIOME_MODIFIER_TYPE = register(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, "remove_carvers", () -> RecordCodecBuilder.mapCodec(builder -> builder.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.RemoveCarversBiomeModifier::biomes),
            WorldCarver.LIST_CODEC.fieldOf("carvers").forGetter(BiomeModifiers.RemoveCarversBiomeModifier::carvers))
            .apply(builder, BiomeModifiers.RemoveCarversBiomeModifier::new)));

    /**
     * Stock biome modifier for adding mob spawn costs to biomes.
     */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.AddSpawnCostsBiomeModifier>> ADD_SPAWN_COSTS_BIOME_MODIFIER_TYPE = register(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, "add_spawn_costs", () -> RecordCodecBuilder.mapCodec(builder -> builder.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.AddSpawnCostsBiomeModifier::biomes),
            RegistryCodecs.holderSet(Registries.ENTITY_TYPE).fieldOf("entity_types").forGetter(BiomeModifiers.AddSpawnCostsBiomeModifier::entityTypes),
            MobSpawnSettings.MobSpawnCost.CODEC.fieldOf("spawn_cost").forGetter(BiomeModifiers.AddSpawnCostsBiomeModifier::spawnCost)).apply(builder, BiomeModifiers.AddSpawnCostsBiomeModifier::new)));

    /**
     * Stock biome modifier for removing mob spawn costs from biomes.
     */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<BiomeModifiers.RemoveSpawnCostsBiomeModifier>> REMOVE_SPAWN_COSTS_BIOME_MODIFIER_TYPE = register(NeoForgeRegistries.BIOME_MODIFIER_SERIALIZERS, "remove_spawn_costs", () -> RecordCodecBuilder.mapCodec(builder -> builder.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(BiomeModifiers.RemoveSpawnCostsBiomeModifier::biomes),
            RegistryCodecs.holderSet(Registries.ENTITY_TYPE).fieldOf("entity_types").forGetter(BiomeModifiers.RemoveSpawnCostsBiomeModifier::entityTypes)).apply(builder, BiomeModifiers.RemoveSpawnCostsBiomeModifier::new)));

    /**
     * Stock structure modifier for adding mob spawns to structures.
     */
    public static final DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<StructureModifiers.AddSpawnsStructureModifier>> ADD_SPAWNS_STRUCTURE_MODIFIER_TYPE = register(NeoForgeRegistries.STRUCTURE_MODIFIER_SERIALIZERS, "add_spawns", () -> RecordCodecBuilder.mapCodec(builder -> builder.group(
            RegistryCodecs.holderSet(Registries.STRUCTURE, Structure.DIRECT_CODEC).fieldOf("structures").forGetter(StructureModifiers.AddSpawnsStructureModifier::structures),
            // Allow either a list or single spawner, attempting to decode the list format first.
            // Uses the better EitherCodec that logs both errors if both formats fail to parse.
            Codec.either(WeightedList.codec(SpawnerData.CODEC), Weighted.codec(SpawnerData.CODEC)).xmap(
                    either -> either.map(Function.identity(), WeightedList::<SpawnerData>of), // convert list/singleton to list when decoding
                    list -> list.unwrap().size() == 1 ? Either.right(list.unwrap().get(0)) : Either.left(list) // convert list to singleton/list when encoding
            ).fieldOf("spawners").forGetter(StructureModifiers.AddSpawnsStructureModifier::spawners)).apply(builder, StructureModifiers.AddSpawnsStructureModifier::new)));

    /**
     * Stock structure modifier for removing mob spawns from structures.
     */
    public static final DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<StructureModifiers.RemoveSpawnsStructureModifier>> REMOVE_SPAWNS_STRUCTURE_MODIFIER_TYPE = register(NeoForgeRegistries.STRUCTURE_MODIFIER_SERIALIZERS, "remove_spawns", () -> RecordCodecBuilder.mapCodec(builder -> builder.group(
            RegistryCodecs.holderSet(Registries.STRUCTURE, Structure.DIRECT_CODEC).fieldOf("structures").forGetter(StructureModifiers.RemoveSpawnsStructureModifier::structures),
            RegistryCodecs.holderSet(Registries.ENTITY_TYPE).fieldOf("entity_types").forGetter(StructureModifiers.RemoveSpawnsStructureModifier::entityTypes)).apply(builder, StructureModifiers.RemoveSpawnsStructureModifier::new)));

    /**
     * Stock structure modifier for removing spawn override lists from structures.
     */
    public static final DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<StructureModifiers.ClearSpawnsStructureModifier>> CLEAR_SPAWNS_STRUCTURE_MODIFIER_TYPE = register(NeoForgeRegistries.STRUCTURE_MODIFIER_SERIALIZERS, "clear_spawns", () -> RecordCodecBuilder.mapCodec(builder -> builder.group(
            RegistryCodecs.holderSet(Registries.STRUCTURE, Structure.DIRECT_CODEC).fieldOf("structures").forGetter(StructureModifiers.ClearSpawnsStructureModifier::structures),
            Codec.<List<MobCategory>, MobCategory>either(MobCategory.CODEC.listOf(), MobCategory.CODEC).<Set<MobCategory>>xmap(
                    either -> either.map(Set::copyOf, Set::of), // convert list/singleton to set when decoding
                    set -> set.size() == 1 ? Either.right(set.toArray(MobCategory[]::new)[0]) : Either.left(List.copyOf(set))).optionalFieldOf("categories", EnumSet.allOf(MobCategory.class)).forGetter(StructureModifiers.ClearSpawnsStructureModifier::categories))
            .apply(builder, StructureModifiers.ClearSpawnsStructureModifier::new)));

    /**
     * Stock holder set type that represents any/all values in a registry. Can be used in a holderset object with {@code { "type": "neoforge:any" }}
     */

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<AndCondition>> AND_CONDITION = register(NeoForgeRegistries.CONDITION_SERIALIZERS, "and", () -> AndCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<NeverCondition>> NEVER_CONDITION = register(NeoForgeRegistries.CONDITION_SERIALIZERS, "never", () -> NeverCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<RegisteredCondition<?>>> REGISTERED_CONDITION = register(NeoForgeRegistries.CONDITION_SERIALIZERS, "registered", () -> RegisteredCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<ModLoadedCondition>> MOD_LOADED_CONDITION = register(NeoForgeRegistries.CONDITION_SERIALIZERS, "mod_loaded", () -> ModLoadedCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<NotCondition>> NOT_CONDITION = register(NeoForgeRegistries.CONDITION_SERIALIZERS, "not", () -> NotCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<OrCondition>> OR_CONDITION = register(NeoForgeRegistries.CONDITION_SERIALIZERS, "or", () -> OrCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<TagEmptyCondition<?>>> TAG_EMPTY_CONDITION = register(NeoForgeRegistries.CONDITION_SERIALIZERS, "tag_empty", () -> TagEmptyCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<AlwaysCondition>> ALWAYS_CONDITION = register(NeoForgeRegistries.CONDITION_SERIALIZERS, "always", () -> AlwaysCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<FeatureFlagsEnabledCondition>> FEATURE_FLAGS_ENABLED_CONDITION = register(NeoForgeRegistries.CONDITION_SERIALIZERS, "feature_flags_enabled", () -> FeatureFlagsEnabledCondition.CODEC);

    public static final Holder<HolderSetType> ANY_HOLDER_SET = Registry.registerForHolder(NeoForgeRegistries.HOLDER_SET_TYPES,
          Identifier.fromNamespaceAndPath(MOD_ID, "any"), new AnyHolderSet.Type());

    public static final Holder<TicketType> GENERATE_FORCED_TICKET = registerTicket("generate_forced", new TicketType(0L, TicketType.FLAG_LOADING));
    public static final Holder<TicketType> BLOCK_TICKET = registerTicket("block", new TicketType(0L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));
    public static final Holder<TicketType> BLOCK_WITH_NATURAL_SPAWNING_TICKET = registerTicket("block_with_natural_spawning",
          new TicketType(0L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));
    public static final Holder<TicketType> ENTITY_TICKET = registerTicket("entity", new TicketType(0L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));
    public static final Holder<TicketType> ENTITY_WITH_NATURAL_SPAWNING_TICKET = registerTicket("entity_with_natural_spawning",
          new TicketType(0L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));

    /**
     * NeoForge's optional milk fluid; on other loaders this is only bound if another mod registers {@code minecraft:milk}.
     */
    public static final DeferredHolder<Fluid, Fluid> MILK = DeferredHolder.create(Registries.FLUID, Identifier.withDefaultNamespace("milk"));
    public static final DeferredHolder<Fluid, Fluid> FLOWING_MILK = DeferredHolder.create(Registries.FLUID, Identifier.withDefaultNamespace("flowing_milk"));

    private static Holder<TicketType> registerTicket(String name, TicketType type) {
        return Registry.registerForHolder(BuiltInRegistries.TICKET_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, name), type);
    }

    /// If the server is intended to be vanilla-compatible then this attribute must not be applied via item attribute modifiers.
    public static final Holder<Attribute> SWIM_SPEED = registerAttribute("swim_speed", new PercentageAttribute("neoforge.swim_speed", 1.0D, 0.0D, 1024.0D).setSyncable(true));
    /// This attribute controls if the player may use creative flight when not in creative mode.
    public static final Holder<Attribute> CREATIVE_FLIGHT = registerAttribute("creative_flight", new BooleanAttribute("neoforge.creative_flight", false).setSyncable(true));
    /// This attribute controls if an entity may use gliding flight (elytra).
    public static final Holder<Attribute> GLIDING_FLIGHT = registerAttribute("gliding_flight", new BooleanAttribute("neoforge.gliding_flight", false).setSyncable(true));

    private static boolean enableMilkFluid = false;

    /**
     * Requests NeoForge's milk fluid to be registered.
     *
     * @apiNote Mekanism shim: The loader registers the milk fluid if this was called during mod initialization.
     */
    public static void enableMilkFluid() {
        enableMilkFluid = true;
    }

    public static boolean isMilkFluidEnabled() {
        return enableMilkFluid;
    }

    private static Holder<Attribute> registerAttribute(String name, Attribute attribute) {
        return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, Identifier.fromNamespaceAndPath(MOD_ID, name), attribute);
    }

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
