/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.client.color.item;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

/**
 * Returns the tint color of the fluid contained in the item stack.
 * Notably, this is used internally by {@link DynamicFluidContainerModel}.
 * <p>
 * Mekanism shim: Vanilla's {@code FluidModel} holds a {@link BlockTintSource}, which is a {@link FluidTintSource} for models registered
 * through NeoForge's API.
 */
public final class FluidContentsTint implements ItemTintSource {
    public static final FluidContentsTint INSTANCE = new FluidContentsTint();
    public static final MapCodec<FluidContentsTint> MAP_CODEC = MapCodec.unit(INSTANCE);

    private FluidContentsTint() {}

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        var fluid = FluidUtil.getFirstStackContained(stack);
        BlockTintSource tintSource = Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(fluid.getFluid().defaultFluidState())
                .tintSource();
        if (tintSource instanceof FluidTintSource fluidTintSource) {
            return fluidTintSource.colorAsStack(fluid);
        }
        return tintSource != null ? tintSource.color(fluid.getFluid().defaultFluidState().createLegacyBlock()) : -1;
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
