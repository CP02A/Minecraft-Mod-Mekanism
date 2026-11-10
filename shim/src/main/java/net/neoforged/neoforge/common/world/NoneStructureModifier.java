/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Mekanism shim: Structure modifier that does nothing, registered so that the structure modifier serializer registry isn't empty.
 */
public final class NoneStructureModifier implements StructureModifier {

    public static final NoneStructureModifier INSTANCE = new NoneStructureModifier();
    public static final MapCodec<NoneStructureModifier> CODEC = MapCodec.unit(INSTANCE);

    private NoneStructureModifier() {
    }

    @Override
    public void modify(RegistryAccess registries, Holder<Structure> structure, Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder) {
    }

    @Override
    public MapCodec<? extends StructureModifier> codec() {
        return CODEC;
    }
}
