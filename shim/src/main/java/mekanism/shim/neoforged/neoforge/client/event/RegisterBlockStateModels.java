/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.neoforge.client.event;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.Event;
import mekanism.shim.neoforged.fml.event.IModBusEvent;
import mekanism.shim.neoforged.neoforge.client.model.block.CustomBlockModelDefinition;
import mekanism.shim.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fire to register new types of {@link CustomUnbakedBlockStateModel} and {@link CustomBlockModelDefinition}.
 */
public class RegisterBlockStateModels extends Event implements IModBusEvent {
    private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends CustomUnbakedBlockStateModel>> modelIdMapper;
    private final ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends CustomBlockModelDefinition>> defintionIdMapper;

    @ApiStatus.Internal
    public RegisterBlockStateModels(
            ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends CustomUnbakedBlockStateModel>> modelIdMapper,
            ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends CustomBlockModelDefinition>> defintionIdMapper) {
        this.modelIdMapper = modelIdMapper;
        this.defintionIdMapper = defintionIdMapper;
    }

    public void registerModel(Identifier location, MapCodec<? extends CustomUnbakedBlockStateModel> codec) {
        this.modelIdMapper.put(location, codec);
    }

    public void registerDefinition(Identifier location, MapCodec<? extends CustomBlockModelDefinition> codec) {
        this.defintionIdMapper.put(location, codec);
    }
}
