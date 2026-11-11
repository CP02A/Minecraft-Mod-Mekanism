/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.client.extensions;

import net.minecraft.Optionull;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.jspecify.annotations.Nullable;

public interface ModelBakerExtension {
    /**
     * Create a {@link ResolvedModel} from an inline {@link UnbakedModel} wrapped by another {@link UnbakedModel}.
     * <p>
     * If the outer model wraps multiple inline models or does not forward the inline model's
     * {@linkplain UnbakedModel#parent() parent} for other reasons, then the inline model's parent
     * must be marked as a dependency in {@link ResolvableModel#resolveDependencies(ResolvableModel.Resolver)}.
     * Additionally, if the inline model may itself be a custom model with non-standard dependencies, then
     * {@link ResolvableModel#resolveDependencies(ResolvableModel.Resolver)} must be called on it as well.
     *
     * @param inlineModel The inline model to be resolved
     * @param debugName   The {@link ModelDebugName} to use for identifying the model in error logging
     * @return a {@link ResolvedModel} to use for baking the inline model
     */
    default ResolvedModel resolveInlineModel(UnbakedModel inlineModel, ModelDebugName debugName) {
        ResolvedModel resolvedParent = Optionull.map(inlineModel.parent(), self()::getModel);
        return new ResolvedModel() {
            @Override
            public UnbakedModel wrapped() {
                return inlineModel;
            }

            @Nullable
            @Override
            public ResolvedModel parent() {
                return resolvedParent;
            }

            @Override
            public String debugName() {
                return debugName.debugName();
            }

            /**
             * NeoForge patches {@link ResolvedModel#bakeTopGeometry} to pass the model's additional properties (root transform, part visibility) to the
             * geometry. Vanilla's default method doesn't on Fabric, so do it here (inline models are for example the children of composite models).
             */
            @Override
            public QuadCollection bakeTopGeometry(TextureSlots textureSlots, ModelBaker baker, ModelState state) {
                return getTopGeometry().bake(textureSlots, baker, state, this, getTopAdditionalProperties());
            }
        };
    }

    private ModelBaker self() {
        return (ModelBaker) this;
    }
}
