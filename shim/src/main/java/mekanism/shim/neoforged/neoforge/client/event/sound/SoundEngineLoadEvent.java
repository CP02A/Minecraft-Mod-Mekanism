/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.neoforge.client.event.sound;

import net.minecraft.client.sounds.SoundEngine;
import net.neoforged.bus.api.ICancellableEvent;
import mekanism.shim.neoforged.fml.LogicalSide;
import mekanism.shim.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired when the {@link SoundEngine} is constructed or (re)loaded, such as during game initialization or when the sound
 * output device is changed.
 *
 * <p>This event is not {@linkplain ICancellableEvent cancellable}, and does not {@linkplain HasResult have a result}.</p>
 *
 * <p>This event is fired on the mod-specific event bus, only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
 */
public class SoundEngineLoadEvent extends SoundEvent implements IModBusEvent {
    @ApiStatus.Internal
    public SoundEngineLoadEvent(SoundEngine manager) {
        super(manager);
    }
}
