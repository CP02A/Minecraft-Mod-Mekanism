/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.shim.ShimServices;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: The subset of FML's mod container used by the shared code. Instances are created by the loader for the mods using the shim.
 */
public abstract class ModContainer {

    protected final String modId;
    protected final String namespace;
    protected final IModInfo modInfo;
    protected final Map<Class<? extends IExtensionPoint>, Supplier<?>> extensionPoints = new IdentityHashMap<>();

    public ModContainer(IModInfo info) {
        this.modInfo = info;
        this.modId = info.getModId();
        this.namespace = info.getNamespace();
    }

    public final String getModId() {
        return modId;
    }

    public final String getNamespace() {
        return namespace;
    }

    public IModInfo getModInfo() {
        return modInfo;
    }

    @SuppressWarnings("unchecked")
    public <T extends IExtensionPoint> Optional<T> getCustomExtension(Class<T> point) {
        Supplier<?> supplier = extensionPoints.get(point);
        return supplier == null ? Optional.empty() : Optional.ofNullable((T) supplier.get());
    }

    public <T extends IExtensionPoint> void registerExtensionPoint(Class<T> point, T extension) {
        registerExtensionPoint(point, (Supplier<T>) () -> extension);
    }

    public <T extends IExtensionPoint> void registerExtensionPoint(Class<T> point, Supplier<T> extension) {
        extensionPoints.put(point, extension);
    }

    public void registerConfig(ModConfig.Type type, IConfigSpec configSpec) {
        ShimServices.BACKEND.registerConfig(this, type, configSpec, null);
    }

    public void registerConfig(ModConfig.Type type, IConfigSpec configSpec, @Nullable String fileName) {
        ShimServices.BACKEND.registerConfig(this, type, configSpec, fileName);
    }

    public abstract IEventBus getEventBus();
}
