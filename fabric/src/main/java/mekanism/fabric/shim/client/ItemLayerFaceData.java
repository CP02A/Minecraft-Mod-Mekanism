package mekanism.fabric.shim.client;

import mekanism.fabric.mixin.client.ItemModelGeneratorAccessor;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;

/**
 * NeoForge's item layer face data ({@code neoforge_data} of {@code layerN} textures, see {@link NeoForgeModelProperties#getItemLayerFaceData}) for
 * generated item models ({@link ItemModelGenerator}, wrapped by {@code ItemModelGeneratorMixin}). Like {@code UnbakedElementsHelper}, only the light
 * emission is applied, as vanilla's face baking has no {@link ExtraFaceData} support (color, per face sky/block light and ambient occlusion).
 */
public final class ItemLayerFaceData {

    private ItemLayerFaceData() {
    }

    public static UnbakedGeometry wrap(UnbakedGeometry original) {
        return (ExtendedUnbakedGeometry) (textureSlots, baker, state, name, additionalProperties) -> {
            if (!hasFaceData(additionalProperties)) {
                return original.bake(textureSlots, baker, state, name);
            }
            return bake(textureSlots, baker, state, name, additionalProperties);
        };
    }

    private static boolean hasFaceData(ContextMap additionalProperties) {
        for (int layerIndex = 0; layerIndex < ItemModelGenerator.LAYERS.size(); layerIndex++) {
            if (NeoForgeModelProperties.getItemLayerFaceData(additionalProperties, layerIndex).lightEmission() != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * NeoForge's version of {@code ItemModelGenerator#bake}, which bakes each layer with its face data.
     */
    private static QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState state, ModelDebugName name, ContextMap additionalProperties) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (int layerIndex = 0; layerIndex < ItemModelGenerator.LAYERS.size(); layerIndex++) {
            Material material = textureSlots.getMaterial(ItemModelGenerator.LAYERS.get(layerIndex));
            if (material == null) {
                break;
            }
            Material.Baked bakedMaterial = baker.materials().get(material, name);
            int lightEmission = NeoForgeModelProperties.getItemLayerFaceData(additionalProperties, layerIndex).lightEmission();
            builder.addAll(baker.compute(new LayerKey(bakedMaterial, state, layerIndex, lightEmission)));
        }
        return builder.build();
    }

    /**
     * Like vanilla's {@link ItemModelGenerator.ItemLayerKey}, with the layer's light emission.
     */
    private record LayerKey(Material.Baked material, ModelState modelState, int layerIndex, int lightEmission)
          implements ModelBaker.SharedOperationKey<QuadCollection> {

        @Override
        public QuadCollection compute(ModelBaker baker) {
            QuadCollection.Builder builder = new QuadCollection.Builder();
            BakedQuad.MaterialInfo materialInfo = baker.interner().materialInfo(BakedQuad.MaterialInfo.of(material, material.sprite().transparency(),
                  layerIndex, null, lightEmission));
            ItemModelGeneratorAccessor.mekanism$bakeExtrudedSprite(builder, baker.interner(), modelState, materialInfo);
            return builder.build();
        }
    }
}
