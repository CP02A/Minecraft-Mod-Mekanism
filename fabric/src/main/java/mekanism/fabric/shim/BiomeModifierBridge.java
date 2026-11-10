package mekanism.fabric.shim;

import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.biome.v1.BiomeModificationContext;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;

/**
 * Loads NeoForge's data driven biome modifiers ({@code data/<namespace>/neoforge/biome_modifier}) and applies them through Fabric's biome modification
 * API.
 * <p>
 * Only NeoForge's builtin feature adding and removing modifiers are supported.
 */
public final class BiomeModifierBridge {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Identifier> WARNED = new HashSet<>();

    private BiomeModifierBridge() {
    }

    public static void register() {
        DynamicRegistries.register(NeoForgeRegistries.Keys.BIOME_MODIFIERS, BiomeModifier.DIRECT_CODEC);
        DynamicRegistries.register(NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS, StructureModifier.DIRECT_CODEC);
        BiomeModifications.create(Identifier.fromNamespaceAndPath("neoforge", "biome_modifiers"))
              .add(ModificationPhase.ADDITIONS, context -> true, (selection, modification) -> apply(selection, modification, false))
              .add(ModificationPhase.REMOVALS, context -> true, (selection, modification) -> apply(selection, modification, true));
    }

    private static void apply(BiomeSelectionContext selection, BiomeModificationContext modification, boolean removals) {
        Optional<Registry<BiomeModifier>> registry = selection.getRegistryAccess().lookup(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        if (registry.isEmpty()) {
            return;
        }
        Holder<net.minecraft.world.level.biome.Biome> biome = selection.getBiomeHolder();
        for (Holder.Reference<BiomeModifier> holder : registry.get().listElements().toList()) {
            BiomeModifier modifier = holder.value();
            if (modifier instanceof BiomeModifiers.AddFeaturesBiomeModifier(var biomes, var features, var step)) {
                if (!removals && biomes.contains(biome)) {
                    for (Holder<PlacedFeature> feature : features) {
                        feature.unwrapKey().ifPresent(key -> modification.getGenerationSettings().addFeature(step, key));
                    }
                }
            } else if (modifier instanceof BiomeModifiers.RemoveFeaturesBiomeModifier(var biomes, var features, var steps)) {
                if (removals && biomes.contains(biome)) {
                    for (Holder<PlacedFeature> feature : features) {
                        Optional<ResourceKey<PlacedFeature>> key = feature.unwrapKey();
                        if (key.isPresent()) {
                            for (GenerationStep.Decoration step : steps) {
                                modification.getGenerationSettings().removeFeature(step, key.get());
                            }
                        }
                    }
                }
            } else if (WARNED.add(holder.key().identifier())) {
                LOGGER.warn("Unsupported biome modifier {} of type {}, only NeoForge's builtin feature modifiers are supported on Fabric", holder.key().identifier(),
                      modifier.getClass().getName());
            }
        }
    }
}
