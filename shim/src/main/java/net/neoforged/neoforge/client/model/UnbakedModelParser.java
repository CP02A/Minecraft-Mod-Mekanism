/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.client.model;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.HashMap;
import java.util.stream.Collectors;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.obj.ObjLoader;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Holds the registered {@link UnbakedModelLoader}s. The loader backend calls {@link #deserializeWithLoader} from the
 * unbaked model json deserializer, as vanilla doesn't support NeoForge's {@code loader} key.
 */
public class UnbakedModelParser {
    private static ImmutableMap<Identifier, UnbakedModelLoader<?>> LOADERS = ImmutableMap.of();

    @Nullable
    public static UnbakedModelLoader<?> get(Identifier name) {
        return LOADERS.get(name);
    }

    @ApiStatus.Internal
    public static void init() {
        var loaders = new HashMap<Identifier, UnbakedModelLoader<?>>();
        //Mekanism shim: NeoForge's own loaders (registered by ClientNeoForgeMod on NeoForge)
        var event = new ModelEvent.RegisterLoaders(loaders);
        event.register(Identifier.fromNamespaceAndPath("neoforge", "empty"), EmptyModel.LOADER);
        event.register(Identifier.fromNamespaceAndPath("neoforge", "obj"), ObjLoader.INSTANCE);
        event.register(Identifier.fromNamespaceAndPath("neoforge", "composite"), CompositeUnbakedModel.Loader.INSTANCE);
        ModLoader.postEvent(event);
        LOADERS = ImmutableMap.copyOf(loaders);
    }

    /**
     * {@return the model read by the loader specified in the json, or null if the json doesn't specify a loader (or the optional loader is missing)}
     */
    @Nullable
    @ApiStatus.Internal
    public static UnbakedModel deserializeWithLoader(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonObject.has("loader")) {
            final Identifier loader;
            final boolean optional;
            if (jsonObject.get("loader").isJsonObject()) {
                JsonObject loaderObject = jsonObject.getAsJsonObject("loader");
                loader = Identifier.parse(GsonHelper.getAsString(loaderObject, "id"));
                optional = GsonHelper.getAsBoolean(loaderObject, "optional", false);
            } else {
                loader = Identifier.parse(GsonHelper.getAsString(jsonObject, "loader"));
                optional = false;
            }

            var loaderInstance = UnbakedModelParser.get(loader);
            if (loaderInstance != null) {
                return loaderInstance.read(jsonObject, jsonDeserializationContext);
            }
            if (!optional) {
                throw new JsonParseException("Unknown loader: " + loader + " (did you forget to register it?) Available loaders: " + LOADERS.keySet().stream().map(Identifier::toString).collect(Collectors.joining(", ")));
            }
        }
        return null;
    }
}
