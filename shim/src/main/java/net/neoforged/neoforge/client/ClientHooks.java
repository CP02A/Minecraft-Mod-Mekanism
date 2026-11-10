/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.client;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.gui.Font;
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

    /**
     * Mekanism shim: NeoForge fires a sound event here; on other loaders the sound is played unchanged.
     */
    @Nullable
    public static SoundInstance playSound(SoundEngine manager, SoundInstance sound) {
        return sound;
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
