/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.extensions.IRenderStateExtension;
import net.neoforged.neoforge.shim.ShimHooks;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: NeoForge's {@code BaseRenderState}, which NeoForge makes render states extend, injected as an interface.
 *
 * @apiNote The data is stored by the shim and cleared by the loader whenever the render state is extracted again.
 */
public interface IBaseRenderStateExtension extends IRenderStateExtension {

    ContextKey<Object2BooleanMap<String>> MODEL_PART_VISIBILITY = new ContextKey<>(Identifier.fromNamespaceAndPath("neoforge", "model_part_visibility"));

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    default <T> T getRenderData(ContextKey<T> key) {
        return (T) ShimHooks.getRenderData(this).get(key);
    }

    @Override
    default <T> void setRenderData(ContextKey<T> key, @Nullable T data) {
        if (data != null) {
            ShimHooks.getRenderData(this).put(key, data);
        } else {
            ShimHooks.getRenderData(this).remove(key);
        }
    }

    default void resetRenderData() {
        ShimHooks.getRenderData(this).clear();
    }

    default void overrideModelPartVisibility(String modelPart, boolean visible) {
        Object2BooleanMap<String> visibility = getRenderData(MODEL_PART_VISIBILITY);
        if (visibility == null) {
            visibility = new Object2BooleanOpenHashMap<>();
            setRenderData(MODEL_PART_VISIBILITY, visibility);
        }
        visibility.put(modelPart, visible);
    }

    @Nullable
    default Object2BooleanMap<String> getModelPartVisibility() {
        return getRenderData(MODEL_PART_VISIBILITY);
    }
}
