/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link TextureAtlasSprite}, injected as an interface.
 */
public interface ITextureAtlasSpriteExtension {

    /**
     * {@return the ARGB color of the pixel of the given frame}
     */
    default int getPixelARGB(int frameIndex, int x, int y) {
        SpriteContents contents = ((TextureAtlasSprite) this).contents();
        if (contents.animatedTexture != null) {
            x += contents.animatedTexture.getFrameX(frameIndex) * contents.width();
            y += contents.animatedTexture.getFrameY(frameIndex) * contents.height();
        }
        return contents.originalImage.getPixel(x, y);
    }
}
