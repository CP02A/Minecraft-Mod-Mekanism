package mekanism.fabric.mixin.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.codec.HolderSetCodec;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.holdersets.ICustomHolderSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds support for NeoForge's custom holder sets ({@code {"type": "neoforge:or", ...}}) to vanilla's holder set codec, like NeoForge does.
 */
@Mixin(HolderSetCodec.class)
public abstract class HolderSetCodecMixin<E> {

    @Shadow
    @Final
    private ResourceKey<? extends Registry<E>> registryKey;

    @Unique
    private Codec<ICustomHolderSet<E>> mekanism$customCodec;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mekanism$createCustomCodec(ResourceKey<? extends Registry<E>> registryKey, Codec<Holder<E>> holderCodec, boolean forceList, CallbackInfo ci) {
        //Note: Lazily initialized, as holder set codecs are created during bootstrap before the shim's registries exist
        this.mekanism$customCodec = Codec.lazyInitialized(() -> NeoForgeRegistries.HOLDER_SET_TYPES.byNameCodec()
              .dispatch(ICustomHolderSet::type, type -> type.makeCodec(registryKey, holderCodec, forceList)));
    }

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private <T> void mekanism$decodeCustom(DynamicOps<T> ops, T input, CallbackInfoReturnable<DataResult<Pair<HolderSet<E>, T>>> cir) {
        DataResult<MapLike<T>> map = ops.getMap(input);
        if (map.isSuccess() && map.getOrThrow().get("type") != null) {
            cir.setReturnValue(mekanism$customCodec.decode(ops, input).map(pair -> pair.mapFirst(set -> set)));
        }
    }

    @Inject(method = "encode(Lnet/minecraft/core/HolderSet;Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;", at = @At("HEAD"),
          cancellable = true)
    private <T> void mekanism$encodeCustom(HolderSet<E> input, DynamicOps<T> ops, T prefix, CallbackInfoReturnable<DataResult<T>> cir) {
        if (input instanceof ICustomHolderSet<E> custom) {
            cir.setReturnValue(mekanism$customCodec.encode(custom, ops, prefix));
        }
    }
}
