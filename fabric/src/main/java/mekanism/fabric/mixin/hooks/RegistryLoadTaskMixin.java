package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Lifecycle;
import mekanism.fabric.shim.FabricShimBackend;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The default key of datapack registries (NeoForge's {@code RegistryBuilder#defaultKey} in the registry's configurator, e.g. the robit skins' base skin),
 * like NeoForge's patch of {@code RegistryLoadTask}, which creates the registry that datapack (and synced) entries are loaded into with the registry's
 * {@code RegistryBuilder}: a registry with a default key is a {@link DefaultedMappedRegistry}.
 */
@Mixin(targets = "net.minecraft.resources.RegistryLoadTask")
public abstract class RegistryLoadTaskMixin {

    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/Lifecycle;)Lnet/minecraft/core/MappedRegistry;"))
    private <T> MappedRegistry<T> mekanism$defaultedRegistry(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, Operation<MappedRegistry<T>> original) {
        Identifier defaultKey = FabricShimBackend.datapackDefaultKey(key);
        return defaultKey == null ? original.call(key, lifecycle) : new DefaultedMappedRegistry<>(defaultKey.toString(), key, lifecycle, false);
    }
}
