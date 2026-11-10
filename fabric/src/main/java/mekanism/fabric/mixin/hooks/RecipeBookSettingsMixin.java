package mekanism.fabric.mixin.hooks;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.world.inventory.RecipeBookType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stores the settings of modded recipe book types (added via enum extension), which vanilla's exhaustive switches don't handle, like NeoForge does.
 * <p>
 * Note: Unlike on NeoForge, the settings of modded types are not saved or synced to the client.
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
}
