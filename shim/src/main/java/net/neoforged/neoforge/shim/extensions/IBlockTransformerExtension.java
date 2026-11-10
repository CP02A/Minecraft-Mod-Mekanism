/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link BlockTransformer}, injected as an interface.
 */
public interface IBlockTransformerExtension {

    /**
     * Transforms the clicked block with this transformer.
     *
     * @param transformer The holder of this transformer, NeoForge uses it to look up the block transforms added by data maps (which the shim doesn't
     *                    support, so it is only validated).
     */
    default InteractionResult transformBlock(UseOnContext context, @Nullable Holder<BlockTransformer> transformer) {
        BlockTransformer self = (BlockTransformer) (Object) this;
        if (transformer != null && self != transformer.value()) {
            throw new IllegalArgumentException("BlockTransformer#transformBlock should only be called with its own transformer");
        }
        return self.transformBlock(context);
    }
}
