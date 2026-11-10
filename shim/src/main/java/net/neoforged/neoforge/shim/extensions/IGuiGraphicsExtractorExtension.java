/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link GuiGraphicsExtractor}, injected as an interface.
 *
 * @implNote The item stack parameters of the tooltip overloads are only used by NeoForge's tooltip events, which are not fired.
 */
public interface IGuiGraphicsExtractorExtension {

    private GuiGraphicsExtractor self() {
        return (GuiGraphicsExtractor) this;
    }

    default void submitGuiElementRenderState(GuiElementRenderState renderState) {
        self().guiRenderState.addGuiElement(renderState);
    }

    default void submitPictureInPictureRenderState(PictureInPictureRenderState renderState) {
        self().guiRenderState.addPicturesInPictureState(renderState);
    }

    @Nullable
    default ScreenRectangle peekScissorStack() {
        return self().scissorStack.peek();
    }

    default void setTooltipForNextFrame(Font font, List<Component> texts, Optional<TooltipComponent> optionalImage, ItemStack stack, int mouseX, int mouseY) {
        self().setTooltipForNextFrame(font, texts, optionalImage, mouseX, mouseY);
    }

    default void setTooltipForNextFrame(Font font, List<Component> texts, Optional<TooltipComponent> optionalImage, ItemStack stack, int mouseX, int mouseY,
          @Nullable Identifier style) {
        self().setTooltipForNextFrame(font, texts, optionalImage, mouseX, mouseY, style);
    }

    default void setComponentTooltipForNextFrame(Font font, List<Component> lines, int mouseX, int mouseY, ItemStack stack) {
        self().setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    default void setComponentTooltipForNextFrame(Font font, List<Component> lines, int mouseX, int mouseY, ItemStack stack, @Nullable Identifier style) {
        self().setComponentTooltipForNextFrame(font, lines, mouseX, mouseY, style);
    }
}
