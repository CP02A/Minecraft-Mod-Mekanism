package mekanism.fabric.shim;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Wraps a NeoForge style custom ingredient as a Fabric custom ingredient.
 */
public record ShimCustomIngredient(ICustomIngredient custom) implements CustomIngredient {

    private static final Map<IngredientType<?>, Serializer<?>> SERIALIZERS = new ConcurrentHashMap<>();

    /**
     * Registers a Fabric serializer for each of the registered NeoForge style ingredient types.
     */
    public static void registerSerializers() {
        for (IngredientType<?> type : NeoForgeRegistries.INGREDIENT_TYPES) {
            serializer(type);
        }
    }

    private static Serializer<?> serializer(IngredientType<?> type) {
        return SERIALIZERS.computeIfAbsent(type, t -> {
            Identifier id = Objects.requireNonNull(NeoForgeRegistries.INGREDIENT_TYPES.getKey(t), "Unregistered ingredient type");
            Serializer<?> serializer = new Serializer<>(id, t);
            CustomIngredientSerializer.register(serializer);
            return serializer;
        });
    }

    @Override
    public boolean test(ItemStack stack) {
        return custom.test(stack);
    }

    @Override
    public Stream<Holder<Item>> items() {
        return custom.items();
    }

    @Override
    public boolean requiresTesting() {
        return !custom.isSimple();
    }

    @Override
    public SlotDisplay display() {
        return custom.display();
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return serializer(custom.getType());
    }

    private static final class Serializer<T extends ICustomIngredient> implements CustomIngredientSerializer<ShimCustomIngredient> {

        private final Identifier id;
        private final MapCodec<ShimCustomIngredient> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ShimCustomIngredient> streamCodec;

        @SuppressWarnings("unchecked")
        private Serializer(Identifier id, IngredientType<T> type) {
            this.id = id;
            this.codec = type.codec().xmap(ShimCustomIngredient::new, ingredient -> (T) ingredient.custom());
            this.streamCodec = ((StreamCodec<RegistryFriendlyByteBuf, T>) type.streamCodec()).map(ShimCustomIngredient::new, ingredient -> (T) ingredient.custom());
        }

        @Override
        public Identifier getIdentifier() {
            return id;
        }

        @Override
        public MapCodec<ShimCustomIngredient> getCodec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShimCustomIngredient> getStreamCodec() {
            return streamCodec;
        }
    }
}
