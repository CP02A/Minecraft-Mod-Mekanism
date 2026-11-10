package mekanism.fabric.shim;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mekanism.fabric.mixin.hooks.RecipeMapAccessor;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Syncs the recipes of the types requested via {@link net.neoforged.neoforge.event.OnDatapackSyncEvent#sendRecipes} to clients, which then fire
 * {@link net.neoforged.neoforge.client.event.RecipesReceivedEvent} (like NeoForge's {@code RecipeContentPayload}).
 */
public final class RecipeContentSync {

    private RecipeContentSync() {
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().registerLarge(Payload.TYPE, Payload.STREAM_CODEC, 64 * 1024 * 1024);
    }

    public static void send(ServerPlayer player, Set<RecipeType<?>> recipeTypes, Collection<RecipeHolder<?>> recipes) {
        if (ServerPlayNetworking.canSend(player, Payload.TYPE)) {
            ServerPlayNetworking.send(player, Payload.create(recipeTypes, recipes));
        }
    }

    /**
     * Creates a client side recipe map from the received recipes (NeoForge's {@code RecipeMap#createClient}).
     */
    public static RecipeMap createClientMap(List<RecipeHolder<?>> recipes) {
        ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byType = ImmutableMultimap.builder();
        ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey = ImmutableMap.builder();
        for (RecipeHolder<?> recipe : recipes) {
            byType.put(recipe.value().getType(), recipe);
            byKey.put(recipe.id(), recipe);
        }
        return RecipeMapAccessor.mekanism$create(byType.build(), byKey.build());
    }

    public record Payload(Set<RecipeType<?>> recipeTypes, List<RecipeHolder<?>> recipes) implements CustomPacketPayload {

        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("neoforge", "recipe_content"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
              ByteBufCodecs.registry(Registries.RECIPE_TYPE).apply(ByteBufCodecs.collection(HashSet::new)), Payload::recipeTypes,
              RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list()), Payload::recipes,
              Payload::new
        );

        static Payload create(Set<RecipeType<?>> recipeTypes, Collection<RecipeHolder<?>> recipes) {
            Set<RecipeType<?>> types = Set.copyOf(recipeTypes);
            if (types.isEmpty()) {
                return new Payload(types, List.of());
            }
            return new Payload(types, recipes.stream().filter(holder -> types.contains(holder.value().getType())).toList());
        }

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }
}
