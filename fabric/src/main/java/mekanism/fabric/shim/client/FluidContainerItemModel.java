package mekanism.fabric.shim.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.resources.Identifier;

/**
 * Simplified version of NeoForge's dynamic fluid container item model ({@code neoforge:fluid_container}), which renders the base texture's item model
 * (for example the empty bucket) without the contained fluid, as the full model depends on NeoForge's custom render types.
 */
public final class FluidContainerItemModel {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("neoforge", "fluid_container");
    private static final MapCodec<ItemModel.Unbaked> CODEC = Codec.unboundedMap(Codec.STRING, Identifier.CODEC).fieldOf("textures")
          .xmap(FluidContainerItemModel::fromTextures, model -> {
              throw new UnsupportedOperationException("Only used for decoding");
          });

    private FluidContainerItemModel() {
    }

    private static ItemModel.Unbaked fromTextures(Map<String, Identifier> textures) {
        Identifier base = textures.getOrDefault("base", Identifier.withDefaultNamespace("item/bucket"));
        return new CuboidItemModelWrapper.Unbaked(base, Optional.empty(), List.of());
    }

    public static void register() {
        ItemModels.ID_MAPPER.put(ID, CODEC);
    }
}
