/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

/**
 * Mekanism shim: The method NeoForge exposes on {@link net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener}, injected as an
 * interface.
 */
public interface IServerConfigurationPacketListenerExtension {

    /**
     * Call when a configuration task is finished.
     */
    default void finishCurrentTask(ConfigurationTask.Type task) {
        ((ServerConfigurationPacketListenerImpl) this).finishCurrentTask(task);
    }
}
