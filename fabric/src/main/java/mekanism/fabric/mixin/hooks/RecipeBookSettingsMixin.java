package mekanism.fabric.mixin.hooks;

import com.google.common.base.CaseFormat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.world.inventory.RecipeBookType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stores, saves and syncs the settings of modded recipe book types (added via enum extension), which vanilla's exhaustive switches don't handle, like
 * NeoForge does ({@code CommonHooks#makeModdedRecipeBookTypesSettingsCodec} and {@code MODDED_RECIPE_BOOK_TYPES_SETTINGS_STREAM_CODEC}).
 */
@Mixin(RecipeBookSettings.class)
public abstract class RecipeBookSettingsMixin {

    @Unique
    private final Map<RecipeBookType, RecipeBookSettings.TypeSettings> mekanism$moddedSettings = new EnumMap<>(RecipeBookType.class);

    @Unique
    private static boolean mekanism$isModded(RecipeBookType type) {
        return switch (type.name()) {
            case "CRAFTING", "FURNACE", "BLAST_FURNACE", "SMOKER" -> false;
            default -> true;
        };
    }

    @Inject(method = "getSettings", at = @At("HEAD"), cancellable = true)
    private void mekanism$getModdedSettings(RecipeBookType type, CallbackInfoReturnable<RecipeBookSettings.TypeSettings> cir) {
        if (mekanism$isModded(type)) {
            cir.setReturnValue(mekanism$moddedSettings.getOrDefault(type, RecipeBookSettings.TypeSettings.DEFAULT));
        }
    }

    @Inject(method = "updateSettings", at = @At("HEAD"), cancellable = true)
    private void mekanism$updateModdedSettings(RecipeBookType type, UnaryOperator<RecipeBookSettings.TypeSettings> operator, CallbackInfo ci) {
        if (mekanism$isModded(type)) {
            mekanism$moddedSettings.put(type, operator.apply(mekanism$moddedSettings.getOrDefault(type, RecipeBookSettings.TypeSettings.DEFAULT)));
            ci.cancel();
        }
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void mekanism$copyModdedSettings(CallbackInfoReturnable<RecipeBookSettings> cir) {
        ((RecipeBookSettingsMixin) (Object) cir.getReturnValue()).mekanism$moddedSettings.putAll(mekanism$moddedSettings);
    }

    @Inject(method = "replaceFrom", at = @At("TAIL"))
    private void mekanism$replaceModdedSettings(RecipeBookSettings other, CallbackInfo ci) {
        mekanism$moddedSettings.clear();
        mekanism$moddedSettings.putAll(((RecipeBookSettingsMixin) (Object) other).mekanism$moddedSettings);
    }

    @Unique
    private static List<RecipeBookType> mekanism$moddedTypes() {
        return Arrays.stream(RecipeBookType.values()).filter(RecipeBookSettingsMixin::mekanism$isModded).toList();
    }

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/codec/StreamCodec;composite(Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;)Lnet/minecraft/network/codec/StreamCodec;"))
    private static StreamCodec<FriendlyByteBuf, RecipeBookSettings> mekanism$syncModdedSettings(StreamCodec<FriendlyByteBuf, RecipeBookSettings> original) {
        return new StreamCodec<>() {
            @Override
            public RecipeBookSettings decode(FriendlyByteBuf buf) {
                RecipeBookSettings settings = original.decode(buf);
                Map<RecipeBookType, RecipeBookSettings.TypeSettings> modded = ((RecipeBookSettingsMixin) (Object) settings).mekanism$moddedSettings;
                for (RecipeBookType type : mekanism$moddedTypes()) {
                    modded.put(type, RecipeBookSettings.TypeSettings.STREAM_CODEC.decode(buf));
                }
                return settings;
            }

            @Override
            public void encode(FriendlyByteBuf buf, RecipeBookSettings settings) {
                original.encode(buf, settings);
                Map<RecipeBookType, RecipeBookSettings.TypeSettings> modded = ((RecipeBookSettingsMixin) (Object) settings).mekanism$moddedSettings;
                for (RecipeBookType type : mekanism$moddedTypes()) {
                    RecipeBookSettings.TypeSettings.STREAM_CODEC.encode(buf, modded.getOrDefault(type, RecipeBookSettings.TypeSettings.DEFAULT));
                }
            }
        };
    }

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;mapCodec(Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;"))
    private static MapCodec<RecipeBookSettings> mekanism$saveModdedSettings(MapCodec<RecipeBookSettings> original) {
        return Codec.mapPair(original, mekanism$moddedSettingsCodec()).xmap(pair -> {
            ((RecipeBookSettingsMixin) (Object) pair.getFirst()).mekanism$moddedSettings.putAll(pair.getSecond());
            return pair.getFirst();
        }, settings -> Pair.of(settings, ((RecipeBookSettingsMixin) (Object) settings).mekanism$moddedSettings));
    }

    @Unique
    private static MapCodec<Map<RecipeBookType, RecipeBookSettings.TypeSettings>> mekanism$moddedSettingsCodec() {
        Map<RecipeBookType, MapCodec<RecipeBookSettings.TypeSettings>> codecs = new EnumMap<>(RecipeBookType.class);
        for (RecipeBookType type : mekanism$moddedTypes()) {
            String name = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, type.name());
            codecs.put(type, RecipeBookSettings.TypeSettings.codec("is" + name + "GuiOpen", "is" + name + "FilteringCraftable"));
        }
        return new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return codecs.values().stream().flatMap(codec -> codec.keys(ops));
            }

            @Override
            public <T> DataResult<Map<RecipeBookType, RecipeBookSettings.TypeSettings>> decode(DynamicOps<T> ops, MapLike<T> input) {
                Map<RecipeBookType, RecipeBookSettings.TypeSettings> map = new EnumMap<>(RecipeBookType.class);
                codecs.forEach((type, codec) -> codec.decode(ops, input).result().ifPresent(settings -> map.put(type, settings)));
                return DataResult.success(map);
            }

            @Override
            public <T> RecordBuilder<T> encode(Map<RecipeBookType, RecipeBookSettings.TypeSettings> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                for (Map.Entry<RecipeBookType, MapCodec<RecipeBookSettings.TypeSettings>> entry : codecs.entrySet()) {
                    prefix = entry.getValue().encode(input.getOrDefault(entry.getKey(), RecipeBookSettings.TypeSettings.DEFAULT), ops, prefix);
                }
                return prefix;
            }
        };
    }
}
