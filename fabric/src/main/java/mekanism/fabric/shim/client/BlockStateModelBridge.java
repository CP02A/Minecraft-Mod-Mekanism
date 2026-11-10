package mekanism.fabric.shim.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;
import net.neoforged.neoforge.client.model.block.CustomBlockModelDefinition;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

/**
 * Supports NeoForge's custom block state models ({@code {"type": "<model type>", ...}} in blockstate files) by registering them as Fabric custom
 * block state models, and rewriting their type key to Fabric's when the blockstate files are loaded.
 */
public final class BlockStateModelBridge {

    private static final Set<String> NEOFORGE_TYPES = ConcurrentHashMap.newKeySet();
    private static final Map<MapCodec<?>, MapCodec<Wrapper>> WRAPPER_CODECS = new ConcurrentHashMap<>();

    private BlockStateModelBridge() {
    }

    public static void register() {
        //The id mapper doesn't expose its entries, so register the models with Fabric as they are put into it
        ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends CustomUnbakedBlockStateModel>> models = new ExtraCodecs.LateBoundIdMapper<>() {
            @Override
            public ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends CustomUnbakedBlockStateModel>> put(Identifier id, MapCodec<? extends CustomUnbakedBlockStateModel> codec) {
                BlockStateModelBridge.register(id, codec);
                return super.put(id, codec);
            }
        };
        ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends CustomBlockModelDefinition>> definitions = new ExtraCodecs.LateBoundIdMapper<>();
        ModLoader.postEvent(new RegisterBlockStateModels(models, definitions));
    }

    private static <M extends CustomUnbakedBlockStateModel> void register(Identifier id, MapCodec<M> codec) {
        MapCodec<Wrapper> wrapperCodec = codec.xmap(Wrapper::new, wrapper -> {
            @SuppressWarnings("unchecked")
            M model = (M) wrapper.model();
            return model;
        });
        WRAPPER_CODECS.put(codec, wrapperCodec);
        NEOFORGE_TYPES.add(id.toString());
        if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            NEOFORGE_TYPES.add(id.getPath());
        }
        net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel.register(id, wrapperCodec);
    }

    /**
     * Rewrites the type key of NeoForge's custom block state models in a blockstate file to Fabric's.
     */
    public static JsonElement rewrite(JsonElement element) {
        if (NEOFORGE_TYPES.isEmpty()) {
            return element;
        }
        if (element instanceof JsonObject object) {
            if (!object.has("fabric:type") && object.get("type") instanceof JsonElement type && type.isJsonPrimitive() && NEOFORGE_TYPES.contains(type.getAsString())) {
                object.add("fabric:type", object.remove("type"));
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                rewrite(entry.getValue());
            }
        } else if (element instanceof JsonArray array) {
            for (JsonElement child : array) {
                rewrite(child);
            }
        }
        return element;
    }

    /**
     * Fabric custom block state model wrapping a NeoForge one.
     */
    private record Wrapper(CustomUnbakedBlockStateModel model) implements net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel {

        @Override
        public MapCodec<? extends net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel> codec() {
            return WRAPPER_CODECS.get(model.codec());
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            return model.bake(baker);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            model.resolveDependencies(resolver);
        }
    }
}
