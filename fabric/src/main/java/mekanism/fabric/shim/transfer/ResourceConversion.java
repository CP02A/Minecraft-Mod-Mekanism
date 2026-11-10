package mekanism.fabric.shim.transfer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;

/**
 * Conversion between NeoForge's resources and Fabric's transfer variants.
 *
 * @param scale The number of Fabric units per NeoForge unit (Fabric measures fluids in droplets, of which there are 81 per millibucket).
 */
public record ResourceConversion<R extends Resource, V extends TransferVariant<?>>(R emptyResource, V blankVariant, long scale, Converter<R, V> converter) {

    public static final ResourceConversion<ItemResource, ItemVariant> ITEM = new ResourceConversion<>(ItemResource.EMPTY, ItemVariant.blank(), 1,
          new Converter<>() {
              @Override
              public ItemResource toResource(ItemVariant variant) {
                  return variant.isBlank() ? ItemResource.EMPTY : ItemResource.of(variant.getItem(), variant.getComponentsPatch());
              }

              @Override
              public ItemVariant toVariant(ItemResource resource) {
                  return resource.isEmpty() ? ItemVariant.blank() : ItemVariant.of(resource.getItem(), resource.getComponentsPatch());
              }
          });
    public static final ResourceConversion<FluidResource, FluidVariant> FLUID = new ResourceConversion<>(FluidResource.EMPTY, FluidVariant.blank(),
          FluidConstants.BUCKET / 1000, new Converter<>() {
        @Override
        public FluidResource toResource(FluidVariant variant) {
            return variant.isBlank() ? FluidResource.EMPTY : FluidResource.of(variant.getFluid(), variant.getComponentsPatch());
        }

        @Override
        public FluidVariant toVariant(FluidResource resource) {
            return resource.isEmpty() ? FluidVariant.blank() : FluidVariant.of(resource.getFluid(), resource.getComponentsPatch());
        }
    });

    public R toResource(V variant) {
        return converter.toResource(variant);
    }

    public V toVariant(R resource) {
        return converter.toVariant(resource);
    }

    public interface Converter<R extends Resource, V extends TransferVariant<?>> {

        R toResource(V variant);

        V toVariant(R resource);
    }
}
