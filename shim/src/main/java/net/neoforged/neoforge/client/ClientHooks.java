/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Camera;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.network.Connection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector2ic;
import org.joml.Vector4f;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's client hooks used by the shared code.
 */
public class ClientHooks {

    @Nullable
    public static SoundInstance playSound(SoundEngine manager, SoundInstance sound) {
        PlaySoundEvent e = new PlaySoundEvent(manager, sound);
        NeoForge.EVENT_BUS.post(e);
        return e.getSound();
    }

    public static void fireClientTickPre() {
        NeoForge.EVENT_BUS.post(new ClientTickEvent.Pre());
    }

    public static void fireClientTickPost() {
        NeoForge.EVENT_BUS.post(new ClientTickEvent.Post());
    }

    public static boolean renderSpecificFirstPersonArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, Identifier skinTexture,
          boolean hasSleeve, PlayerRenderState renderState, HumanoidArm arm, ModelPart armPart) {
        return NeoForge.EVENT_BUS.post(new RenderArmEvent(poseStack, submitNodeCollector, lightCoords, skinTexture, hasSleeve, renderState, arm, armPart)).isCanceled();
    }

    public static void getFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, float fogRed, float fogGreen,
          float fogBlue, Vector4f dest) {
        // Modify fog color depending on the fluid
        FluidState state = level.getFluidState(camera.blockPosition());
        dest.set(fogRed, fogGreen, fogBlue, 1F);
        if (camera.position().y < (double) ((float) camera.blockPosition().getY() + state.getHeight(level, camera.blockPosition()))) {
            IClientFluidTypeExtensions.of(state).modifyFogColor(camera, partialTick, level, renderDistance, darkenWorldAmount, dest);
        }
        NeoForge.EVENT_BUS.post(new ViewportEvent.ComputeFogColor(camera, partialTick, dest));
    }

    public static void onSetupFog(@Nullable FogEnvironment environment, FogType type, Camera camera, float partialTick, float renderDistance, FogData fogData) {
        // Modify fog rendering depending on the fluid
        FluidState state = camera.entity().level().getFluidState(camera.blockPosition());
        if (camera.position().y < (double) ((float) camera.blockPosition().getY() + state.getHeight(camera.entity().level(), camera.blockPosition()))) {
            IClientFluidTypeExtensions.of(state).modifyFogRender(camera, environment, renderDistance, partialTick, fogData);
        }
        NeoForge.EVENT_BUS.post(new ViewportEvent.RenderFog(environment, type, camera, partialTick, fogData));
    }

    public static boolean onMouseScroll(MouseHandler mouseHelper, double scrollDeltaX, double scrollDeltaY, Vector2ic acummulatedScroll) {
        var event = new InputEvent.MouseScrollingEvent(scrollDeltaX, scrollDeltaY, acummulatedScroll, mouseHelper.isLeftPressed(), mouseHelper.isMiddlePressed(),
              mouseHelper.isRightPressed(), mouseHelper.xpos(), mouseHelper.ypos());
        return NeoForge.EVENT_BUS.post(event).isCanceled();
    }

    public static void firePlayerLogin(MultiPlayerGameMode pc, LocalPlayer player, Connection networkManager) {
        NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggingIn(pc, player, networkManager));
    }

    public static void firePlayerLogout(@Nullable MultiPlayerGameMode pc, @Nullable LocalPlayer player) {
        NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.LoggingOut(pc, player, player != null ? player.connection != null ? player.connection.getConnection() : null : null));
    }

    public static void firePlayerRespawn(MultiPlayerGameMode pc, LocalPlayer oldPlayer, LocalPlayer newPlayer, Connection networkManager) {
        NeoForge.EVENT_BUS.post(new ClientPlayerNetworkEvent.Clone(pc, oldPlayer, newPlayer, networkManager));
    }

    public static List<ClientTooltipComponent> gatherTooltipComponents(ItemStack stack, List<? extends FormattedText> textElements, int mouseX, int screenWidth,
          int screenHeight, Font font) {
        int tooltipTextWidth = textElements.stream().mapToInt(font::width).max().orElse(0);
        boolean needsWrap = false;
        int tooltipX = mouseX + 12;
        if (tooltipX + tooltipTextWidth + 4 > screenWidth) {
            tooltipX = mouseX - 16 - tooltipTextWidth;
            // if the tooltip doesn't fit on the screen
            if (tooltipX < 4) {
                if (mouseX > screenWidth / 2) {
                    tooltipTextWidth = mouseX - 12 - 8;
                } else {
                    tooltipTextWidth = screenWidth - 16 - mouseX;
                }
                needsWrap = true;
            }
        }
        int tooltipTextWidthF = tooltipTextWidth;
        if (needsWrap) {
            return textElements.stream().flatMap(text -> splitLine(text, font, tooltipTextWidthF)).toList();
        }
        return textElements.stream()
              .map(text -> ClientTooltipComponent.create(text instanceof Component component ? component.getVisualOrderText() : Language.getInstance().getVisualOrder(text)))
              .toList();
    }

    private static Stream<ClientTooltipComponent> splitLine(FormattedText text, Font font, int maxWidth) {
        if (text instanceof Component component && component.getString().isEmpty()) {
            return Stream.of(component.getVisualOrderText()).map(ClientTooltipComponent::create);
        }
        return font.split(text, maxWidth).stream().map(ClientTooltipComponent::create);
    }
}
