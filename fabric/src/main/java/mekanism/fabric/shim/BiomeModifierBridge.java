package mekanism.fabric.shim;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.AttributeModifier;
import net.minecraft.world.attribute.modifier.MobSpawnSettingsModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.common.world.ModifiableStructureInfo;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Loads NeoForge's data driven biome modifiers ({@code data/<namespace>/neoforge/biome_modifier}) and applies them through Fabric's biome modification
 * API, and NeoForge's structure modifiers ({@code data/<namespace>/neoforge/structure_modifier}) through {@code StructureMixin}.
 * <p>
 * Biome modifiers are run against a NeoForge {@link ModifiableBiomeInfo.BiomeInfo.Builder} built from the biome's current state for each Fabric modification
 * phase (NeoForge's {@code BEFORE_EVERYTHING} and {@code ADD} phases in {@link ModificationPhase#ADDITIONS}, {@code REMOVE} in
 * {@link ModificationPhase#REMOVALS}, {@code MODIFY} in {@link ModificationPhase#REPLACEMENTS} and {@code AFTER_EVERYTHING} in
 * {@link ModificationPhase#POST_PROCESSING}). Changes to mob spawns, spawn costs, features, carvers and environment attributes are then applied to Fabric's
 * modification context. Changes to the climate settings or special effects are not supported.
 * <p>
 * Note: Fabric applies biome modifications in the server's constructor, before server configs would be loaded (NeoForge applies them in
 * {@code ServerAboutToStartEvent} after loading server configs), so {@code MinecraftServerConfigMixin} loads them early.
 */
public final class BiomeModifierBridge {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> WARNED = new HashSet<>();

    private BiomeModifierBridge() {
    }

    public static void register() {
        DynamicRegistries.register(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BiomeModifier.DIRECT_CODEC);
        DynamicRegistries.register(NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS, StructureModifier.DIRECT_CODEC);
        BiomeModifications.create(Identifier.fromNamespaceAndPath("neoforge", "biome_modifiers"))
              .add(ModificationPhase.ADDITIONS, context -> true, (selection, modification) -> apply(selection, modification, BiomeModifier.Phase.BEFORE_EVERYTHING,
                    BiomeModifier.Phase.ADD))
              .add(ModificationPhase.REMOVALS, context -> true, (selection, modification) -> apply(selection, modification, BiomeModifier.Phase.REMOVE))
              .add(ModificationPhase.REPLACEMENTS, context -> true, (selection, modification) -> apply(selection, modification, BiomeModifier.Phase.MODIFY))
              .add(ModificationPhase.POST_PROCESSING, context -> true, (selection, modification) -> apply(selection, modification,
                    BiomeModifier.Phase.AFTER_EVERYTHING));
        //Like NeoForge, structure modifiers are applied before the levels are created (and after server configs are loaded)
        ServerLifecycleEvents.SERVER_STARTING.register(BiomeModifierBridge::applyStructureModifiers);
    }

    private static void apply(BiomeSelectionContext selection, BiomeModificationContext modification, BiomeModifier.Phase... phases) {
        RegistryAccess registries = selection.getRegistryAccess();
        Optional<Registry<BiomeModifier>> registry = registries.lookup(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        if (registry.isEmpty() || registry.get().size() == 0) {
            return;
        }
        Biome biome = selection.getBiome();
        Holder<Biome> holder = selection.getBiomeHolder();
        BiomeModificationContext.MobSpawnSettingsContext spawnContext = modification.getMobSpawnSettings();
        BiomeModificationContext.GenerationSettingsContext generationContext = modification.getGenerationSettings();

        //Snapshot of the biome's current state (including the changes of earlier phases and other mods)
        MobSpawnSettings.Builder spawnsBuilder = new MobSpawnSettings.Builder();
        for (MobCategory category : spawnContext.getMobCategories()) {
            spawnsBuilder.addAllSpawns(category, WeightedList.of(spawnContext.getMobs(category)));
        }
        spawnsBuilder.addAllCosts(spawnContext.getMobCharges());
        MobSpawnSettings spawns = spawnsBuilder.build();
        EnvironmentAttributeMap attributes = EnvironmentAttributeMap.builder()
              .putAll(biome.getAttributes())
              .modify(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettingsModifier.overlay(), spawns)
              .build();
        Map<GenerationStep.Decoration, List<Holder<PlacedFeature>>> features = new EnumMap<>(GenerationStep.Decoration.class);
        List<HolderSet<PlacedFeature>> featureSets = new ArrayList<>();
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            List<Holder<PlacedFeature>> stepFeatures = List.copyOf(generationContext.getFeatures(step));
            features.put(step, stepFeatures);
            featureSets.add(HolderSet.direct(stepFeatures));
        }
        List<Holder<WorldCarver>> carvers = List.copyOf(generationContext.getCarvers());
        BiomeGenerationSettings generationSettings = new BiomeGenerationSettings(HolderSet.direct(carvers), featureSets);
        ModifiableBiomeInfo.BiomeInfo original = new ModifiableBiomeInfo.BiomeInfo(biome.climateSettings, attributes, biome.getSpecialEffects(), generationSettings);

        ModifiableBiomeInfo.BiomeInfo.Builder builder = ModifiableBiomeInfo.BiomeInfo.Builder.copyOf(selection.getBiomeKey(), original);
        List<Holder.Reference<BiomeModifier>> modifiers = registry.get().listElements().toList();
        for (BiomeModifier.Phase phase : phases) {
            for (Holder.Reference<BiomeModifier> modifier : modifiers) {
                modifier.value().modify(registries, holder, phase, builder);
            }
        }
        ModifiableBiomeInfo.BiomeInfo modified = builder.build();

        //Mob spawns and spawn costs
        MobSpawnSettings modifiedSpawns = modified.attributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
        for (MobCategory category : MobCategory.values()) {
            WeightedList<MobSpawnSettings.SpawnerData> before = spawns.getMobsInCategory(category);
            WeightedList<MobSpawnSettings.SpawnerData> after = modifiedSpawns.getMobsInCategory(category);
            List<Weighted<MobSpawnSettings.SpawnerData>> beforeList = before == null ? List.of() : before.unwrap();
            List<Weighted<MobSpawnSettings.SpawnerData>> afterList = after == null ? List.of() : after.unwrap();
            if (!beforeList.equals(afterList)) {
                spawnContext.clearSpawns(category);
                for (Weighted<MobSpawnSettings.SpawnerData> spawner : afterList) {
                    spawnContext.addSpawn(category, spawner.value(), spawner.weight());
                }
            }
        }
        Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> costsBefore = spawns.allSpawnCosts();
        Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> costsAfter = modifiedSpawns.allSpawnCosts();
        for (EntityType<?> type : costsBefore.keySet()) {
            if (!costsAfter.containsKey(type)) {
                spawnContext.clearMobCharge(type);
            }
        }
        costsAfter.forEach((type, cost) -> {
            if (!cost.equals(costsBefore.get(type))) {
                spawnContext.addMobCharge(type, cost.charge(), cost.energyBudget());
            }
        });

        //Features and carvers
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            List<Holder<PlacedFeature>> before = features.get(step);
            List<Holder<PlacedFeature>> after = step.ordinal() < modified.generationSettings().features().size()
                                                ? modified.generationSettings().features().get(step.ordinal()).stream().toList() : List.of();
            if (!before.equals(after)) {
                for (Holder<PlacedFeature> feature : before) {
                    if (!after.contains(feature)) {
                        key(feature, "feature").ifPresent(key -> generationContext.removeFeature(step, key));
                    }
                }
                for (Holder<PlacedFeature> feature : after) {
                    if (!before.contains(feature)) {
                        key(feature, "feature").ifPresent(key -> generationContext.addFeature(step, key));
                    }
                }
            }
        }
        List<Holder<WorldCarver>> carversAfter = new ArrayList<>();
        modified.generationSettings().getCarvers().forEach(carversAfter::add);
        for (Holder<WorldCarver> carver : carvers) {
            if (!carversAfter.contains(carver)) {
                key(carver, "carver").ifPresent(generationContext::removeCarver);
            }
        }
        for (Holder<WorldCarver> carver : carversAfter) {
            if (!carvers.contains(carver)) {
                key(carver, "carver").ifPresent(generationContext::addCarver);
            }
        }

        //Environment attributes (other than the mob spawns)
        for (EnvironmentAttribute<?> attribute : modified.attributes().keySet()) {
            if (attribute != EnvironmentAttributes.NATURAL_MOB_SPAWNS) {
                EnvironmentAttributeMap.Entry<?, ?> entry = modified.attributes().get(attribute);
                if (entry != null && !entry.equals(attributes.get(attribute))) {
                    setAttribute(modification, attribute, entry);
                }
            }
        }
        for (EnvironmentAttribute<?> attribute : attributes.keySet()) {
            if (!modified.attributes().contains(attribute)) {
                warn("removal of environment attributes (" + attribute + ")");
            }
        }
        if (!Objects.equals(original.climateSettings(), modified.climateSettings())) {
            warn("changes to climate settings");
        }
        if (!Objects.equals(original.effects(), modified.effects())) {
            warn("changes to special effects");
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, M> void setAttribute(BiomeModificationContext modification, EnvironmentAttribute<T> attribute, EnvironmentAttributeMap.Entry<?, ?> entry) {
        EnvironmentAttributeMap.Entry<T, M> typed = (EnvironmentAttributeMap.Entry<T, M>) entry;
        modification.getAttributes().setModifier(attribute, (AttributeModifier<T, M>) typed.modifier(), typed.argument());
    }

    private static <T> Optional<ResourceKey<T>> key(Holder<T> holder, String type) {
        Optional<ResourceKey<T>> key = holder.unwrapKey();
        if (key.isEmpty()) {
            warn("adding or removing unregistered (direct) " + type + "s");
        }
        return key;
    }

    private static void warn(String unsupported) {
        if (WARNED.add(unsupported)) {
            LOGGER.warn("A NeoForge biome modifier made unsupported {}, which aren't applied on Fabric", unsupported);
        }
    }

    /**
     * Applies NeoForge's structure modifiers, like NeoForge's {@code ServerLifecycleHooks#runModifiers}.
     */
    private static void applyStructureModifiers(MinecraftServer server) {
        RegistryAccess registries = server.registryAccess();
        List<StructureModifier> modifiers = registries.lookup(NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS)
              .map(registry -> registry.listElements().map(Holder::value).toList())
              .orElse(List.of());
        if (modifiers.isEmpty()) {
            return;
        }
        Registry<Structure> structures = registries.lookupOrThrow(Registries.STRUCTURE);
        for (Holder.Reference<Structure> structure : structures.listElements().toList()) {
            Structure value = structure.value();
            ModifiedStructureSettings settingsHolder = (ModifiedStructureSettings) value;
            //Note: Structures are recreated whenever the world gen registries are loaded, but reset in case of reapplying
            settingsHolder.mekanism$setModifiedSettings(null);
            Structure.StructureSettings original = new Structure.StructureSettings(value.biomes(), value.spawnOverrides(), value.step(), value.terrainAdaptation());
            ModifiableStructureInfo info = new ModifiableStructureInfo(new ModifiableStructureInfo.StructureInfo(original));
            info.applyStructureModifiers(registries, structure, modifiers);
            settingsHolder.mekanism$setModifiedSettings(info.get().structureSettings());
        }
    }

    /**
     * Implemented by {@link Structure} (through {@code StructureMixin}): settings returned instead of the original ones, like NeoForge's field redirects to
     * {@code Structure#getModifiedStructureSettings}.
     */
    public interface ModifiedStructureSettings {

        void mekanism$setModifiedSettings(Structure.@Nullable StructureSettings settings);
    }
}
