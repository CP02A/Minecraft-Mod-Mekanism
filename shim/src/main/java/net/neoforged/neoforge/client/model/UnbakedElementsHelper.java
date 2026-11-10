/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.client.model;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import org.joml.Vector3f;

/**
 * Mekanism shim: Subset of NeoForge's {@code UnbakedElementsHelper}.
 */
public final class UnbakedElementsHelper {

    private UnbakedElementsHelper() {
    }

    /**
     * Composes the given root transform into the model state, adjusting the origin of the transform to match how model states are applied.
     */
    public static ModelState composeRootTransformIntoModelState(ModelState modelState, Transformation rootTransform) {
        if (rootTransform.isIdentity()) {
            return modelState;
        }
        // Move the origin of the root transform as if the negative corner were the block center to match the way the
        // ModelState transform is applied in the FaceBakery by moving the vertices to be centered on that corner
        rootTransform = rootTransform.applyOrigin(new Vector3f(-.5F, -.5F, -.5F));
        return new ComposedModelState(modelState, rootTransform);
    }
}
