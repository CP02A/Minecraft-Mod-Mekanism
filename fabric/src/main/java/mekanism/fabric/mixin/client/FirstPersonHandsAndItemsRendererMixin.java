package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's first person arm render event.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {

    @WrapOperation(method = "renderPlayerHand", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"))
    private void mekanism$renderRightArm(AvatarRenderer<?> renderer, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, Identifier skinTexture,
          boolean hasSleeve, Operation<Void> original, PoseStack ps, SubmitNodeCollector c, int l, HumanoidArm arm, PlayerRenderState playerState) {
        if (!ClientHooks.renderSpecificFirstPersonArm(poseStack, collector, lightCoords, skinTexture, hasSleeve, playerState, HumanoidArm.RIGHT, renderer.getModel().rightArm)) {
            original.call(renderer, poseStack, collector, lightCoords, skinTexture, hasSleeve);
        }
    }

    @WrapOperation(method = "renderPlayerHand", at = @At(value = "INVOKE",
          target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"))
    private void mekanism$renderLeftArm(AvatarRenderer<?> renderer, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, Identifier skinTexture,
          boolean hasSleeve, Operation<Void> original, PoseStack ps, SubmitNodeCollector c, int l, HumanoidArm arm, PlayerRenderState playerState) {
        if (!ClientHooks.renderSpecificFirstPersonArm(poseStack, collector, lightCoords, skinTexture, hasSleeve, playerState, HumanoidArm.LEFT, renderer.getModel().leftArm)) {
            original.call(renderer, poseStack, collector, lightCoords, skinTexture, hasSleeve);
        }
    }
}
