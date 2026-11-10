package mekanism.fabric.mixin.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Evaluates NeoForge's resource conditions ({@code neoforge:conditions}) wherever Fabric evaluates its own resource conditions.
 */
@Mixin(value = ResourceConditionsImpl.class, remap = false)
public abstract class ResourceConditionsImplMixin {

    @Inject(method = "applyResourceConditions", at = @At("HEAD"), cancellable = true)
    private static void mekanism$applyNeoForgeConditions(JsonObject obj, String dataType, Identifier key, RegistryOps.@Nullable RegistryInfoLookup registryInfo,
          CallbackInfoReturnable<Boolean> cir) {
        JsonElement conditions = obj.get(ConditionalOps.DEFAULT_CONDITIONS_KEY);
        if (conditions == null) {
            return;
        }
        List<ICondition> parsed = ICondition.LIST_CODEC.parse(JsonOps.INSTANCE, conditions).getOrThrow(error -> new IllegalStateException(
              "Failed to parse NeoForge conditions of " + dataType + " " + key + ": " + error));
        ICondition.IContext context = new ICondition.IContext() {
            @SuppressWarnings("unchecked")
            private <T> Optional<HolderSet.Named<T>> getNamed(TagKey<T> tag) {
                if (registryInfo == null) {
                    return Optional.empty();
                }
                return registryInfo.lookup((ResourceKey<? extends Registry<? extends T>>) tag.registry()).flatMap(getter -> getter.get(tag));
            }

            @Override
            public <T> boolean isTagLoaded(TagKey<T> tag) {
                return getNamed(tag).isPresent();
            }

            @Override
            public <T> Collection<Holder<T>> getTag(TagKey<T> tag) {
                return getNamed(tag).filter(HolderSet.Named::isBound).<Collection<Holder<T>>>map(HolderSet.Named::contents).orElse(List.of());
            }

            @Override
            public HolderGetter.Provider registries() {
                return new HolderGetter.Provider() {
                    @Override
                    public <T> Optional<? extends HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
                        return registryInfo == null ? Optional.empty() : registryInfo.lookup(registryKey);
                    }
                };
            }

            @Override
            public FeatureFlagSet enabledFeatures() {
                FeatureFlagSet features = ResourceConditionsImpl.currentFeatures;
                return features == null ? FeatureFlags.VANILLA_SET : features;
            }
        };
        for (ICondition condition : parsed) {
            if (!condition.test(context)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
