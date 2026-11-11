package mekanism.fabric.shim;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.lang.annotation.ElementType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.core.Registry;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.fml.event.lifecycle.ModLifecycleEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import mekanism.fabric.shim.transfer.CapabilityBridge;
import net.neoforged.neoforge.capabilities.CapabilityHooks;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.tooltip.ItemTooltipHandler;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.RegisterCauldronInteractionEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.shim.ShimHooks;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

/**
 * Loads the mods that are written against NeoForge's API (via the shim) on Fabric: constructs them, registers their automatic event subscribers and
 * fires NeoForge's mod loading events at the appropriate times.
 * <p>
 * Mods declare a {@code neoforge-shim} entrypoint implementing {@link ShimModInitializer}.
 */
public final class ShimModLoader {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String ENTRYPOINT = "neoforge-shim";
    public static final String CLIENT_ENTRYPOINT = "neoforge-shim-client";
    private static final Type EVENT_BUS_SUBSCRIBER = Type.getType(EventBusSubscriber.class);

    private static boolean loaded;
    private static List<FabricModContainer> containers = List.of();

    /**
     * {@return the containers of the loaded shim mods}
     */
    public static List<FabricModContainer> getContainers() {
        return containers;
    }
    private static Map<ResourceKey<Registry<?>>, Map<Identifier, DataMapType<?, ?>>> dataMapTypes = Map.of();

    private ShimModLoader() {
    }

    /**
     * {@return the data map types registered by the shim mods, by registry}
     */
    public static Map<ResourceKey<Registry<?>>, Map<Identifier, DataMapType<?, ?>>> getDataMapTypes() {
        return dataMapTypes;
    }

    /**
     * Loads all shim mods, if they have not been loaded yet. Called from the main entrypoint of the first shim mod that initializes.
     */
    public static synchronized void loadAll() {
        if (loaded) {
            return;
        }
        loaded = true;
        NeoForgeMod.init();
        List<EntrypointContainer<ShimModInitializer>> entrypoints = sortByDependencies(FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT, ShimModInitializer.class));
        List<FabricModContainer> containers = new ArrayList<>();
        for (EntrypointContainer<ShimModInitializer> entrypoint : entrypoints) {
            net.fabricmc.loader.api.ModContainer fabricMod = entrypoint.getProvider();
            FabricModContainer container = new FabricModContainer(new FabricModList.FabricModInfo(fabricMod.getMetadata()));
            ModFileScanData scanData = new ModFileScanData();
            ModScanner.scan(fabricMod.getRootPaths(), scanData);
            scanData.addModFileInfo(() -> List.of(container.getModInfo()));
            FabricModList.INSTANCE.addContainer(container, scanData);
            containers.add(container);
            LOGGER.info("Constructing NeoForge shim mod {}", container.getModId());
            entrypoint.getEntrypoint().construct(container, container.getEventBus());
        }
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            //Equivalent of NeoForge's client only @Mod classes, constructed after all common ones
            for (EntrypointContainer<ShimModInitializer> entrypoint : sortByDependencies(FabricLoader.getInstance().getEntrypointContainers(CLIENT_ENTRYPOINT, ShimModInitializer.class))) {
                FabricModContainer container = FabricModList.INSTANCE.getModContainerById(entrypoint.getProvider().getMetadata().getId())
                      .orElseThrow(() -> new IllegalStateException("Client entrypoint of " + entrypoint.getProvider().getMetadata().getId() + " without a common entrypoint"));
                entrypoint.getEntrypoint().construct(container, container.getEventBus());
            }
        }
        for (FabricModContainer container : containers) {
            registerAutomaticSubscribers(container, FabricModList.INSTANCE.getAllScanData().get(containers.indexOf(container)));
        }
        ShimModLoader.containers = List.copyOf(containers);

        //Registries
        NewRegistryEvent newRegistryEvent = new NewRegistryEvent();
        ModLoader.postEvent(newRegistryEvent);
        newRegistryEvent.fill();
        BiomeModifierBridge.register();
        NewDatapackRegistryEvent datapackRegistryEvent = new NewDatapackRegistryEvent();
        ModLoader.postEvent(datapackRegistryEvent);
        datapackRegistryEvent.process();
        for (Identifier registryName : getRegistrationOrder()) {
            Registry<?> registry = BuiltInRegistries.REGISTRY.getValue(registryName);
            if (registry != null) {
                ModLoader.postEvent(new RegisterEvent(registry.key(), registry));
            }
        }
        FabricShimBackend.bakeRegistries();
        ShimCustomIngredient.registerSerializers();
        FabricAttachmentBackend.registerTypes();

        Map<ResourceKey<Registry<?>>, Map<Identifier, DataMapType<?, ?>>> dataMaps = new HashMap<>();
        ModLoader.postEvent(new RegisterDataMapTypesEvent(dataMaps));
        dataMapTypes = dataMaps;
        DataMapLoader.register();
        RecipeContentSync.register();
        AdvancedMenuOpening.register();

        ModLoader.postEvent(new RegisterPayloadHandlersEvent());
        FabricNetworkBackend.registerConfigurationTasks();
        RegisterCapabilitiesEvent capabilitiesEvent = new RegisterCapabilitiesEvent();
        CapabilityHooks.markProxyableCapabilities(capabilitiesEvent);
        CapabilityHooks.registerVanillaProviders(capabilitiesEvent);
        ModLoader.postEvent(capabilitiesEvent);
        CapabilityHooks.registerFallbackVanillaProviders(capabilitiesEvent);
        ShimHooks.capabilitiesInitialized = true;
        CapabilityBridge.register();
        ForcedChunkManager.init();

        Map<String, CauldronInteraction.Dispatcher> dispatchers = new LinkedHashMap<>();
        dispatchers.put("empty", CauldronInteractions.EMPTY);
        dispatchers.put("water", CauldronInteractions.WATER);
        dispatchers.put("lava", CauldronInteractions.LAVA);
        dispatchers.put("powder_snow", CauldronInteractions.POWDER_SNOW);
        ModLoader.postEvent(new RegisterCauldronInteractionEvent.Dispatcher(dispatchers));
        ModLoader.postEvent(new RegisterCauldronInteractionEvent.Interaction(dispatchers));

        Map<EntityType<? extends LivingEntity>, AttributeSupplier> attributes = new IdentityHashMap<>();
        ModLoader.postEvent(new EntityAttributeCreationEvent(attributes));
        attributes.forEach((type, supplier) -> FabricDefaultAttributeRegistry.register(type, supplier));

        ServerHooks.registerSpawnPlacements();
        ItemTooltipHandler.init();

        DefaultItemComponentEvents.MODIFY.register(context -> {
            Map<Item, ModifyDefaultComponentsEvent.Initializer> byItem = new Object2ObjectOpenHashMap<>();
            List<Pair<ModifyDefaultComponentsEvent.ItemWithComponentsPredicate, ModifyDefaultComponentsEvent.Initializer>> byPredicate = new ArrayList<>();
            ModLoader.postEvent(new ModifyDefaultComponentsEvent(byItem, byPredicate));
            byItem.forEach((item, initializer) -> context.modify(item, (builder, lookup, i) -> initializer.run(builder, lookup, i)));
            for (Pair<ModifyDefaultComponentsEvent.ItemWithComponentsPredicate, ModifyDefaultComponentsEvent.Initializer> pair : byPredicate) {
                context.modify(item -> pair.getFirst().test(item, item.components()), (builder, lookup, item) -> pair.getSecond().run(builder, lookup, item));
            }
        });

        GameEventBridge.register();
        CreativeTabBridge.register();

        for (FabricModContainer container : containers) {
            postLifecycle(container, new FMLCommonSetupEvent(container));
        }
        if (FMLEnvironment.getDist() == Dist.DEDICATED_SERVER) {
            for (FabricModContainer container : containers) {
                postLifecycle(container, new FMLDedicatedServerSetupEvent(container));
            }
        }
        NeoForge.EVENT_BUS.start();
        AddServerReloadListenersEvent reloadListenersEvent = NeoForge.EVENT_BUS.post(new AddServerReloadListenersEvent());
        reloadListenersEvent.getListeners().forEach((id, listener) -> ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id, listener));
        //IMC and load complete are fired once all mods (including those that don't use the shim) have initialized
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ServerLifecycleHooks.setCurrentServer(server));
        Runnable finish = () -> {
            for (FabricModContainer container : containers) {
                postLifecycle(container, new InterModEnqueueEvent(container));
            }
            for (FabricModContainer container : containers) {
                postLifecycle(container, new InterModProcessEvent(container));
            }
            for (FabricModContainer container : containers) {
                postLifecycle(container, new FMLLoadCompleteEvent(container));
            }
        };
        if (FMLEnvironment.getDist() == Dist.DEDICATED_SERVER) {
            ServerLifecycleEvents.SERVER_STARTING.register(server -> runOnce(finish));
        } else {
            CLIENT_FINISH = finish;
        }
    }

    /**
     * {@return the order the registry events are fired in, which matches NeoForge's order}
     */
    private static Set<Identifier> getRegistrationOrder() {
        Set<Identifier> ordered = new LinkedHashSet<>();
        //Vanilla's order is incorrect, as items and mob effects depend on attributes, items depend on data components, and blocks and mob effects depend on
        // particle types at construction time
        ordered.add(Registries.ATTRIBUTE.identifier());
        ordered.add(Registries.DATA_COMPONENT_TYPE.identifier());
        ordered.add(Registries.PARTICLE_TYPE.identifier());
        //Vanilla registries in the order they were created, followed by modded registries sorted by name
        //Note: Iterate the registry itself as its key set is unordered
        for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
            Identifier name = registry.key().identifier();
            if (name.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                ordered.add(name);
            }
        }
        ordered.addAll(BuiltInRegistries.REGISTRY.keySet().stream().sorted(java.util.Comparator.comparing(Identifier::getNamespace).thenComparing(Identifier::getPath)).toList());
        return ordered;
    }

    /**
     * Sorts the entrypoints so that mods are constructed after the shim mods they depend on (like NeoForge's dependency ordering), as Fabric doesn't
     * guarantee an order. Keeps the original order otherwise.
     */
    private static <T> List<EntrypointContainer<T>> sortByDependencies(List<EntrypointContainer<T>> entrypoints) {
        Map<String, List<EntrypointContainer<T>>> byMod = new LinkedHashMap<>();
        for (EntrypointContainer<T> entrypoint : entrypoints) {
            byMod.computeIfAbsent(entrypoint.getProvider().getMetadata().getId(), id -> new ArrayList<>()).add(entrypoint);
        }
        List<EntrypointContainer<T>> sorted = new ArrayList<>(entrypoints.size());
        Set<String> visited = new LinkedHashSet<>();
        for (String modId : byMod.keySet()) {
            visitDependencies(modId, byMod, visited, sorted);
        }
        return sorted;
    }

    private static <T> void visitDependencies(String modId, Map<String, List<EntrypointContainer<T>>> byMod, Set<String> visited, List<EntrypointContainer<T>> sorted) {
        if (!visited.add(modId)) {
            return;
        }
        List<EntrypointContainer<T>> modEntrypoints = byMod.get(modId);
        for (net.fabricmc.loader.api.metadata.ModDependency dependency : modEntrypoints.getFirst().getProvider().getMetadata().getDependencies()) {
            if (dependency.getKind().isPositive() && byMod.containsKey(dependency.getModId())) {
                visitDependencies(dependency.getModId(), byMod, visited, sorted);
            }
        }
        sorted.addAll(modEntrypoints);
    }

    private static Runnable CLIENT_FINISH;
    private static boolean finished;

    private static synchronized void runOnce(Runnable finish) {
        if (!finished) {
            finished = true;
            finish.run();
        }
    }

    /**
     * Fires the IMC and load complete events on the client, once the client has initialized.
     */
    public static void finishClientLoading() {
        if (CLIENT_FINISH != null) {
            runOnce(CLIENT_FINISH);
        }
    }

    private static void postLifecycle(FabricModContainer container, ModLifecycleEvent event) {
        container.getEventBus().post(event);
    }

    @SuppressWarnings("unchecked")
    private static void registerAutomaticSubscribers(FabricModContainer container, ModFileScanData scanData) {
        Dist dist = FMLEnvironment.getDist();
        for (ModFileScanData.AnnotationData data : scanData.getAnnotations()) {
            if (data.targetType() != ElementType.TYPE || !data.annotationType().equals(EVENT_BUS_SUBSCRIBER)) {
                continue;
            }
            String modId = (String) data.annotationData().getOrDefault("modid", container.getModId());
            if (!modId.isEmpty() && !modId.equals(container.getModId())) {
                continue;
            }
            List<ModAnnotation.EnumHolder> sides = (List<ModAnnotation.EnumHolder>) data.annotationData().get("value");
            if (sides != null && sides.stream().noneMatch(side -> side.value().equals(dist.name()))) {
                continue;
            }
            String className = data.clazz().getClassName();
            try {
                Class<?> clazz = Class.forName(className, true, ShimModLoader.class.getClassLoader());
                registerStaticListeners(clazz, container.getEventBus());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to register automatic event subscriber " + className, e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerStaticListeners(Class<?> clazz, IEventBus modBus) throws IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        for (Method method : clazz.getDeclaredMethods()) {
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            if (annotation == null || !Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> eventType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(eventType)) {
                continue;
            }
            method.setAccessible(true);
            MethodHandle handle = lookup.unreflect(method);
            Consumer<Event> listener = event -> {
                try {
                    handle.invoke(event);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
            IEventBus bus = IModBusEvent.class.isAssignableFrom(eventType) ? modBus : NeoForge.EVENT_BUS;
            bus.addListener(annotation.priority(), annotation.receiveCanceled(), (Class) eventType, (Consumer) listener);
        }
    }
}
