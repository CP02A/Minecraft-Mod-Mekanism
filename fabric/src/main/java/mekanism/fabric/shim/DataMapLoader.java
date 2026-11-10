package mekanism.fabric.shim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.registries.datamaps.AdvancedDataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.DataMapValueMerger;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import net.neoforged.neoforge.shim.DataMapStorage;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Loads NeoForge style data maps ({@code data/<namespace>/data_maps/<registry>/<name>.json}) for the data map types registered by shim mods, and syncs them
 * to clients.
 * <p>
 * The files are read during the resource reload, and resolved once the tags have been bound, as data map entries may reference tags.
 */
public final class DataMapLoader implements ResourceManagerReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier ID = Identifier.fromNamespaceAndPath("neoforge", "data_maps");
    private static final FileToIdConverter FILES = FileToIdConverter.json("data_maps");

    private static Map<DataMapType<?, ?>, List<JsonObject>> pending = Map.of();

    public static void register() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(ID, new DataMapLoader());
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (!client) {
                resolve(registries);
            }
        });
        PayloadTypeRegistry.clientboundPlay().registerLarge(SyncPayload.TYPE, SyncPayload.STREAM_CODEC, 64 * 1024 * 1024);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            if (ServerPlayNetworking.canSend(player, SyncPayload.TYPE)) {
                ServerPlayNetworking.send(player, SyncPayload.create(player.registryAccess()));
            }
        });
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<DataMapType<?, ?>, List<JsonObject>> files = new HashMap<>();
        for (Map.Entry<Identifier, List<Resource>> entry : FILES.listMatchingResourceStacks(manager).entrySet()) {
            Identifier file = FILES.fileToId(entry.getKey());
            //<registry path (namespace omitted for minecraft)>/<data map name>
            int split = file.getPath().lastIndexOf('/');
            if (split == -1) {
                continue;
            }
            String registryPath = file.getPath().substring(0, split);
            Identifier registryName = registryPath.contains("/") ? Identifier.parse(registryPath.replaceFirst("/", ":")) : Identifier.withDefaultNamespace(registryPath);
            Identifier dataMapId = Identifier.fromNamespaceAndPath(file.getNamespace(), file.getPath().substring(split + 1));
            Map<Identifier, DataMapType<?, ?>> types = ShimModLoader.getDataMapTypes().get(ResourceKey.createRegistryKey(registryName));
            DataMapType<?, ?> type = types == null ? null : types.get(dataMapId);
            if (type == null) {
                //Not a data map of a shim mod (for example one of NeoForge's own data maps)
                continue;
            }
            List<JsonObject> contents = files.computeIfAbsent(type, t -> new ArrayList<>());
            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    contents.add(JsonParser.parseReader(reader).getAsJsonObject());
                } catch (Exception e) {
                    LOGGER.error("Failed to read data map {} from pack {}", dataMapId, resource.sourcePackId(), e);
                }
            }
        }
        pending = files;
    }

    private static void resolve(RegistryAccess registries) {
        DataMapStorage.clear();
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        Map<ResourceKey<? extends Registry<?>>, Registry<?>> updated = new LinkedHashMap<>();
        for (Map.Entry<DataMapType<?, ?>, List<JsonObject>> entry : pending.entrySet()) {
            DataMapType<?, ?> type = entry.getKey();
            Optional<? extends Registry<?>> registry = registries.lookup(type.registryKey());
            if (registry.isPresent()) {
                resolve(registry.get(), type, entry.getValue(), ops);
                updated.put(type.registryKey(), registry.get());
            }
        }
        for (Registry<?> registry : updated.values()) {
            NeoForge.EVENT_BUS.post(new DataMapsUpdatedEvent(registries, registry, DataMapsUpdatedEvent.UpdateCause.SERVER_RELOAD));
        }
    }

    @SuppressWarnings("unchecked")
    private static <R, T> void resolve(Registry<?> rawRegistry, DataMapType<R, T> type, List<JsonObject> files, DynamicOps<JsonElement> ops) {
        Registry<R> registry = (Registry<R>) rawRegistry;
        DataMapValueMerger<R, T> merger = type instanceof AdvancedDataMapType<R, T, ?> advanced ? advanced.merger() : DataMapValueMerger.defaultMerger();
        Map<ResourceKey<R>, T> values = new HashMap<>();
        Map<ResourceKey<R>, Either<TagKey<R>, ResourceKey<R>>> sources = new HashMap<>();
        for (JsonObject file : files) {
            if (!conditionsMet(file, ops)) {
                continue;
            }
            if (file.has("replace") && file.get("replace").getAsBoolean()) {
                values.clear();
                sources.clear();
            }
            JsonElement entries = file.get("values");
            if (entries != null && entries.isJsonObject()) {
                for (Map.Entry<String, JsonElement> value : entries.getAsJsonObject().entrySet()) {
                    JsonElement element = value.getValue();
                    boolean replace = false;
                    if (element.isJsonObject() && element.getAsJsonObject().has("value") && isWrapped(element.getAsJsonObject())) {
                        JsonObject wrapped = element.getAsJsonObject();
                        if (!conditionsMet(wrapped, ops)) {
                            continue;
                        }
                        replace = wrapped.has("replace") && wrapped.get("replace").getAsBoolean();
                        element = wrapped.get("value");
                    }
                    Optional<T> parsed = type.codec().parse(ops, element).resultOrPartial(error -> LOGGER.error("Failed to parse value of {} in data map {}: {}",
                          value.getKey(), type.id(), error));
                    if (parsed.isEmpty()) {
                        continue;
                    }
                    Either<TagKey<R>, ResourceKey<R>> source = parseSource(registry, value.getKey());
                    for (ResourceKey<R> key : resolveKeys(registry, source)) {
                        T existing = values.get(key);
                        if (existing == null || replace) {
                            values.put(key, parsed.get());
                        } else {
                            values.put(key, merger.merge(registry, sources.get(key), existing, source, parsed.get()));
                        }
                        sources.put(key, source);
                    }
                }
            }
            JsonElement remove = file.get("remove");
            if (remove != null && remove.isJsonArray()) {
                for (JsonElement removed : (JsonArray) remove) {
                    if (removed.isJsonPrimitive()) {
                        for (ResourceKey<R> key : resolveKeys(registry, parseSource(registry, removed.getAsString()))) {
                            values.remove(key);
                            sources.remove(key);
                        }
                    }
                }
            }
        }
        DataMapStorage.set(type, values);
    }

    private static boolean isWrapped(JsonObject object) {
        for (String key : object.keySet()) {
            if (!key.equals("value") && !key.equals("replace") && !key.equals(ConditionalOps.DEFAULT_CONDITIONS_KEY)) {
                return false;
            }
        }
        return true;
    }

    private static boolean conditionsMet(JsonObject object, DynamicOps<JsonElement> ops) {
        return !object.has(ConditionalOps.DEFAULT_CONDITIONS_KEY) || net.neoforged.neoforge.common.conditions.ICondition.conditionsMatched(ops, object);
    }

    private static <R> Either<TagKey<R>, ResourceKey<R>> parseSource(Registry<R> registry, String key) {
        if (key.startsWith("#")) {
            return Either.left(TagKey.create(registry.key(), Identifier.parse(key.substring(1))));
        }
        return Either.right(ResourceKey.create(registry.key(), Identifier.parse(key)));
    }

    private static <R> List<ResourceKey<R>> resolveKeys(Registry<R> registry, Either<TagKey<R>, ResourceKey<R>> source) {
        return source.map(tag -> registry.get(tag).map(HolderSet.Named::stream).orElseGet(java.util.stream.Stream::empty)
              .map(holder -> holder.unwrapKey().orElse(null)).filter(java.util.Objects::nonNull).toList(), key -> registry.containsKey(key) ? List.of(key) : List.of());
    }

    /**
     * Sync of the data maps that have a network codec.
     */
    public record SyncPayload(CompoundTag data) implements CustomPacketPayload {

        public static final Type<SyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("neoforge", "sync_data_maps"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC = ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap)
              .<RegistryFriendlyByteBuf>cast().map(SyncPayload::new, SyncPayload::data);

        static SyncPayload create(RegistryAccess registries) {
            RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            CompoundTag data = new CompoundTag();
            for (Map<Identifier, DataMapType<?, ?>> types : ShimModLoader.getDataMapTypes().values()) {
                for (DataMapType<?, ?> type : types.values()) {
                    CompoundTag encoded = encode(type, ops);
                    if (encoded != null) {
                        data.put(type.registryKey().identifier() + "|" + type.id(), encoded);
                    }
                }
            }
            return new SyncPayload(data);
        }

        @Nullable
        private static <R, T> CompoundTag encode(DataMapType<R, T> type, RegistryOps<Tag> ops) {
            if (type.networkCodec() == null) {
                return null;
            }
            CompoundTag values = new CompoundTag();
            DataMapStorage.getDataMap(type).forEach((key, value) -> type.networkCodec().encodeStart(ops, value)
                  .ifSuccess(tag -> values.put(key.identifier().toString(), tag)));
            return values;
        }

        /**
         * Applies the synced data maps on the client.
         */
        public void apply(RegistryAccess registries, boolean integratedServer) {
            RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
            Map<ResourceKey<? extends Registry<?>>, Registry<?>> updated = new LinkedHashMap<>();
            for (Map<Identifier, DataMapType<?, ?>> types : ShimModLoader.getDataMapTypes().values()) {
                for (DataMapType<?, ?> type : types.values()) {
                    if (data.get(type.registryKey().identifier() + "|" + type.id()) instanceof CompoundTag values) {
                        //The integrated server shares its data maps with the client
                        if (!integratedServer) {
                            decode(type, values, ops);
                        }
                        registries.lookup(type.registryKey()).ifPresent(registry -> updated.put(type.registryKey(), registry));
                    }
                }
            }
            for (Registry<?> registry : updated.values()) {
                NeoForge.EVENT_BUS.post(new DataMapsUpdatedEvent(registries, registry, DataMapsUpdatedEvent.UpdateCause.CLIENT_SYNC));
            }
        }

        private static <R, T> void decode(DataMapType<R, T> type, CompoundTag values, RegistryOps<Tag> ops) {
            Map<ResourceKey<R>, T> decoded = new HashMap<>();
            for (String key : values.keySet()) {
                type.networkCodec().parse(ops, values.get(key)).ifSuccess(value -> decoded.put(ResourceKey.create(type.registryKey(), Identifier.parse(key)), value));
            }
            DataMapStorage.set(type, decoded);
        }

        @Override
        public Type<SyncPayload> type() {
            return TYPE;
        }
    }
}
