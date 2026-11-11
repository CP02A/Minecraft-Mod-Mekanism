package mekanism.fabric.mixin.hooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import net.neoforged.neoforge.common.tooltip.ItemTooltipHandler;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs NeoForge's tooltip appenders ({@code RegisterTooltipAppendersEvent}) where NeoForge's patch of {@link ItemStack#addDetailsToTooltip} runs them:
 * head appenders before {@link Item#appendHoverText}, post custom ones after it, component appenders after the vanilla component tooltip they are sorted
 * behind, pre item info ones before the advanced item id/component count and tail ones at the end. Vanilla's own component tooltips stay in place.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

    @Inject(method = "addDetailsToTooltip", at = @At("HEAD"))
    private void mekanism$tooltipHead(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag flag, Consumer<Component> builder,
          CallbackInfo ci) {
        ItemTooltipHandler.addDetailsToTooltipHead((ItemStack) (Object) this, context, display, player, flag, builder);
    }

    @Inject(method = "addDetailsToTooltip", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
          target = "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void mekanism$tooltipPostCustom(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag flag,
          Consumer<Component> builder, CallbackInfo ci) {
        ItemTooltipHandler.addDetailsToTooltipPostCustom((ItemStack) (Object) this, context, display, player, flag, builder);
    }

    @WrapOperation(method = "addDetailsToTooltip", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/item/ItemStack;addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void mekanism$afterComponent(ItemStack stack, DataComponentType<?> type, Item.TooltipContext context, TooltipDisplay display,
          Consumer<Component> builder, TooltipFlag flag, Operation<Void> original, @Local(argsOnly = true) @Nullable Player player) {
        original.call(stack, type, context, display, builder, flag);
        ItemTooltipHandler.addDetailsAfterComponent(type, stack, context, display, player, flag, builder);
    }

    @WrapOperation(method = "addDetailsToTooltip", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/item/ItemStack;addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/component/TooltipProvider$Getter;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void mekanism$afterGetterComponent(ItemStack stack, DataComponentType<?> type, TooltipProvider.Getter<?> getter, Item.TooltipContext context,
          TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag, Operation<Void> original, @Local(argsOnly = true) @Nullable Player player) {
        original.call(stack, type, getter, context, display, builder, flag);
        ItemTooltipHandler.addDetailsAfterComponent(type, stack, context, display, player, flag, builder);
    }

    @WrapOperation(method = "addDetailsToTooltip", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/item/ItemStack;addAttributeTooltips(Ljava/util/function/Consumer;Lnet/minecraft/world/item/component/TooltipDisplay;Lnet/minecraft/world/entity/player/Player;)V"))
    private void mekanism$afterAttributes(ItemStack stack, Consumer<Component> builder, TooltipDisplay display, @Nullable Player player, Operation<Void> original,
          @Local(argsOnly = true) Item.TooltipContext context, @Local(argsOnly = true) TooltipFlag flag) {
        original.call(stack, builder, display, player);
        ItemTooltipHandler.addDetailsAfterComponent(DataComponents.ATTRIBUTE_MODIFIERS, stack, context, display, player, flag, builder);
    }

    @WrapOperation(method = "addDetailsToTooltip", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/item/ItemStack;addUnitComponentToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;)V"))
    private void mekanism$afterUnitComponent(ItemStack stack, DataComponentType<?> type, Component component, TooltipDisplay display, Consumer<Component> builder,
          Operation<Void> original, @Local(argsOnly = true) Item.TooltipContext context, @Local(argsOnly = true) @Nullable Player player,
          @Local(argsOnly = true) TooltipFlag flag) {
        original.call(stack, type, component, display, builder);
        ItemTooltipHandler.addDetailsAfterComponent(type, stack, context, display, player, flag, builder);
    }

    @Inject(method = "addDetailsToTooltip", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC, ordinal = 0,
          target = "Lnet/minecraft/core/component/DataComponents;CAN_BREAK:Lnet/minecraft/core/component/DataComponentType;"))
    private void mekanism$afterBlockEntityData(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag flag,
          Consumer<Component> builder, CallbackInfo ci) {
        ItemTooltipHandler.addDetailsAfterComponent(DataComponents.BLOCK_ENTITY_DATA, (ItemStack) (Object) this, context, display, player, flag, builder);
    }

    @Inject(method = "addDetailsToTooltip", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC, ordinal = 0,
          target = "Lnet/minecraft/core/component/DataComponents;CAN_PLACE_ON:Lnet/minecraft/core/component/DataComponentType;"))
    private void mekanism$afterCanBreak(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag flag,
          Consumer<Component> builder, CallbackInfo ci) {
        ItemTooltipHandler.addDetailsAfterComponent(DataComponents.CAN_BREAK, (ItemStack) (Object) this, context, display, player, flag, builder);
    }

    /**
     * Vanilla checks {@link TooltipFlag#isAdvanced()} once for both the durability line and the item id/component count. Appenders after the can place
     * on tooltip run before that check; when the flag isn't advanced the durability line is skipped, so the appenders after it run right away,
     * otherwise they run before the item id ({@link #mekanism$afterDamage}).
     */
    @WrapOperation(method = "addDetailsToTooltip", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/item/TooltipFlag;isAdvanced()Z"))
    private boolean mekanism$afterCanPlaceOn(TooltipFlag flag, Operation<Boolean> original, @Local(argsOnly = true) Item.TooltipContext context,
          @Local(argsOnly = true) TooltipDisplay display, @Local(argsOnly = true) @Nullable Player player, @Local(argsOnly = true) Consumer<Component> builder) {
        ItemStack stack = (ItemStack) (Object) this;
        ItemTooltipHandler.addDetailsAfterComponent(DataComponents.CAN_PLACE_ON, stack, context, display, player, flag, builder);
        boolean advanced = original.call(flag);
        if (!advanced) {
            ItemTooltipHandler.addDetailsAfterComponent(DataComponents.DAMAGE, stack, context, display, player, flag, builder);
        }
        return advanced;
    }

    @Inject(method = "addDetailsToTooltip", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC, ordinal = 0,
          target = "Lnet/minecraft/core/registries/BuiltInRegistries;ITEM:Lnet/minecraft/core/DefaultedRegistry;"))
    private void mekanism$afterDamage(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag flag, Consumer<Component> builder,
          CallbackInfo ci) {
        ItemTooltipHandler.addDetailsAfterComponent(DataComponents.DAMAGE, (ItemStack) (Object) this, context, display, player, flag, builder);
    }

    @Inject(method = "addDetailsToTooltip", at = @At("TAIL"))
    private void mekanism$tooltipTail(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag flag, Consumer<Component> builder,
          CallbackInfo ci) {
        ItemTooltipHandler.addDetailsToTooltipTail((ItemStack) (Object) this, context, display, player, flag, builder);
    }
}
