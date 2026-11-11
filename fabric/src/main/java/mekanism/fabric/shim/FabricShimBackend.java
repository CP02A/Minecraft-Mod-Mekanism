package mekanism.fabric.shim;

import com.mojang.serialization.Codec;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistry;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.callback.AddCallback;
import net.neoforged.neoforge.registries.callback.BakeCallback;
import net.neoforged.neoforge.registries.callback.RegistryCallback;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.shim.ShimBackend;
import org.jspecify.annotations.Nullable;

/**
 * Fabric implementation of the shim's core loader services.
 */
public class FabricShimBackend implements ShimBackend {

    private record RegistrySettings<T>(boolean sync, List<RegistryCallback<T>> callbacks) {
    }

    private static final Map<Registry<?>, RegistrySettings<?>> REGISTRY_SETTINGS = new ConcurrentHashMap<>();
    private static final List<Registry<?>> BAKE_ON_COMPLETION = new CopyOnWriteArrayList<>();
    private static final Map<ResourceKey<?>, List<BakeCallback<?>>> DATAPACK_BAKE_CALLBACKS = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<?>, Identifier> DATAPACK_DEFAULT_KEYS = new ConcurrentHashMap<>();

    @Override
    public <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, boolean sync) {
        FabricRegistryBuilder<T, MappedRegistry<T>> builder = FabricRegistryBuilder.create(key);
        if (sync) {
            builder.attribute(RegistryAttribute.SYNCED);
        }
        return builder.buildAndRegister();
    }

    @Override
    public <T> void configureRegistry(Registry<T> registry, boolean sync, List<RegistryCallback<T>> callbacks) {
        REGISTRY_SETTINGS.put(registry, new RegistrySettings<>(sync, callbacks));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void registerRootRegistry(Registry<?> registry) {
        RegistrySettings<?> settings = REGISTRY_SETTINGS.get(registry);
        FabricRegistryBuilder builder = FabricRegistryBuilder.from((MappedRegistry) registry);
        if (settings != null && settings.sync()) {
            builder.attribute(RegistryAttribute.SYNCED);
        }
        builder.buildAndRegister();
        if (settings != null) {
            for (RegistryCallback<?> callback : settings.callbacks()) {
                if (callback instanceof AddCallback addCallback) {
                    registerAddCallback((Registry) registry, addCallback);
                } else if (callback instanceof BakeCallback) {
                    BAKE_ON_COMPLETION.add(registry);
                }
            }
        }
    }

    private static <T> void registerAddCallback(Registry<T> registry, AddCallback<T> callback) {
        RegistryEntryAddedCallback.event(registry).register((rawId, id, value) -> callback.onAdd(registry, rawId, ResourceKey.create(registry.key(), id), value));
    }

    /**
     * Invokes the bake callbacks of the registries created via NeoForge's registry builder, called once all registration has completed.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void bakeRegistries() {
        for (Registry<?> registry : BAKE_ON_COMPLETION) {
            RegistrySettings<?> settings = REGISTRY_SETTINGS.get(registry);
            if (settings != null) {
                for (RegistryCallback<?> callback : settings.callbacks()) {
                    if (callback instanceof BakeCallback bakeCallback) {
                        bakeCallback.onBake(registry);
                    }
                }
            }
        }
    }

    @Override
    public <T> void addAlias(Registry<T> registry, Identifier from, Identifier to) {
        ((FabricRegistry) registry).addAlias(from, to);
    }

    @Override
    public Ingredient toVanilla(ICustomIngredient ingredient) {
        return new ShimCustomIngredient(ingredient).toVanilla();
    }

    @Nullable
    @Override
    public ICustomIngredient getCustomIngredient(Ingredient ingredient) {
        return ingredient.getCustomIngredient() instanceof ShimCustomIngredient custom ? custom.custom() : null;
    }

    @Override
    public boolean isSimple(Ingredient ingredient) {
        CustomIngredient custom = ingredient.getCustomIngredient();
        if (custom == null) {
            return true;
        } else if (custom instanceof ShimCustomIngredient shim) {
            return shim.custom().isSimple();
        }
        return !custom.requiresTesting();
    }

    @Override
    public FluidType getFallbackFluidType(Fluid fluid) {
        FluidType.Properties properties = FluidType.Properties.create();
        if (fluid == Fluids.EMPTY) {
            properties.descriptionId("block.minecraft.air");
        } else {
            FluidVariant variant = FluidVariant.of(fluid);
            properties.descriptionId(fluid.defaultFluidState().createLegacyBlock().getBlock().getDescriptionId())
                  .lightLevel(FluidVariantAttributes.getLuminance(variant))
                  .temperature(FluidVariantAttributes.getTemperature(variant))
                  .viscosity(FluidVariantAttributes.getViscosity(variant, null));
            if (FluidVariantAttributes.isLighterThanAir(variant)) {
                properties.density(-1000);
            }
        }
        return new FluidType(properties);
    }

    @Override
    public Dist getDist() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? Dist.CLIENT : Dist.DEDICATED_SERVER;
    }

    @Override
    public boolean isProduction() {
        return !FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isOnServerThread() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread();
    }

    @Override
    public Path getGameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public ModList getModList() {
        return FabricModList.INSTANCE;
    }

    @Override
    public void registerConfig(ModContainer container, ModConfig.Type type, IConfigSpec spec, @Nullable String fileName) {
        if (fileName == null) {
            ConfigRegistry.INSTANCE.register(container.getModId(), type, spec);
        } else {
            ConfigRegistry.INSTANCE.register(container.getModId(), type, spec, fileName);
        }
    }

    @Nullable
    @Override
    public String getLastKnownUsername(UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        return server.services().nameToIdCache().get(uuid).map(NameAndId::name).orElse(null);
    }

    @Override
    public <T> void addDatapackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec, @Nullable Codec<T> networkCodec, boolean reloadable,
          List<RegistryCallback<T>> callbacks, @Nullable Identifier defaultKey) {
        if (defaultKey != null) {
            DATAPACK_DEFAULT_KEYS.put(key, defaultKey);
        }
        List<BakeCallback<?>> bakeCallbacks = callbacks.stream().<BakeCallback<?>>mapMulti((callback, consumer) -> {
            if (callback instanceof BakeCallback<T> bakeCallback) {
                consumer.accept(bakeCallback);
            }
        }).toList();
        if (!bakeCallbacks.isEmpty()) {
            DATAPACK_BAKE_CALLBACKS.put(key, bakeCallbacks);
        }
        if (reloadable) {
            DynamicRegistries.registerReloadable(key, codec);
        } else if (networkCodec == null) {
            DynamicRegistries.register(key, codec);
        } else {
            DynamicRegistries.registerSynced(key, codec, networkCodec);
        }
    }

    /**
     * {@return the default key of a datapack registry (NeoForge's {@code RegistryBuilder#defaultKey}), or {@code null} if it has none} Used by
     * {@code RegistryLoadTaskMixin} to create the loaded registry as a defaulted registry, like NeoForge's patch of {@code RegistryLoadTask}.
     */
    @Nullable
    public static Identifier datapackDefaultKey(ResourceKey<?> registryKey) {
        return DATAPACK_DEFAULT_KEYS.get(registryKey);
    }

    /**
     * Invokes the bake callbacks of datapack registries, called when a registry gets frozen (NeoForge calls them from {@code MappedRegistry#freeze}, which
     * vanilla calls once a datapack registry has been loaded on the server or received by the client).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onRegistryFrozen(Registry<?> registry) {
        List<BakeCallback<?>> callbacks = DATAPACK_BAKE_CALLBACKS.get(registry.key());
        if (callbacks != null) {
            for (BakeCallback callback : callbacks) {
                callback.onBake(registry);
            }
        }
    }
}
