package mekanism.fabric.mixin.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the ingredient codec accept NeoForge's custom ingredient format ({@code {"neoforge:ingredient_type": "<ingredient type>", ...}}) in addition to vanilla's and Fabric's
 * formats. Custom ingredients are still written in Fabric's format.
 */
@Mixin(Ingredient.class)
public abstract class IngredientMixin {

    @Shadow
    @Final
    @Mutable
    public static Codec<Ingredient> CODEC;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void mekanism$acceptNeoForgeCustomIngredients(CallbackInfo ci) {
        Codec<Ingredient> fabricCodec = CODEC;
        //Note: Lazily initialized, as the ingredient class may be initialized before the shim's registries can be created
        Codec<Ingredient> neoForgeIngredientCodec = Codec.lazyInitialized(() -> {
            MapCodec<ICustomIngredient> neoForgeCodec = NeoForgeRegistries.INGREDIENT_TYPES.byNameCodec().dispatchMap("neoforge:ingredient_type", ICustomIngredient::getType, IngredientType::codec);
            return neoForgeCodec.codec().xmap(ICustomIngredient::toVanilla, ingredient -> {
                throw new IllegalStateException("Only used for decoding");
            });
        });
        CODEC = Codec.withAlternative(fabricCodec, neoForgeIngredientCodec);
    }

    /**
     * Skips validating the contents of holder sets that are not immediately resolvable (NeoForge's custom holder sets containing tags), like NeoForge does.
     */
    @Redirect(method = "<init>(Lnet/minecraft/core/HolderSet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/HolderSet;unwrap()Lcom/mojang/datafixers/util/Either;"))
    private Either<TagKey<Item>, List<Holder<Item>>> mekanism$skipUnresolvableValidation(HolderSet<Item> values) {
        return values.isImmediatelyResolvable() ? values.unwrap() : Either.left(null);
    }
}
