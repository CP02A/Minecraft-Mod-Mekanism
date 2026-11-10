/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.registries;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.shim.ShimServices;
import org.jspecify.annotations.Nullable;

/**
 * Fired to register datapack registries.
 *
 * <p>Mekanism shim: The registries are registered with the loader's dynamic registry system after the event is fired.
 */
public final class NewDatapackRegistryEvent extends Event implements IModBusEvent {

    private final List<RegistryData<?>> worldRegistries = new ArrayList<>();
    private final List<RegistryData<?>> reloadableRegistries = new ArrayList<>();

    public NewDatapackRegistryEvent() {
    }

    public <T> void worldRegistry(ResourceKey<Registry<T>> registryKey, Codec<T> codec) {
        this.worldRegistry(registryKey, codec, null);
    }

    public <T> void worldRegistry(ResourceKey<Registry<T>> registryKey, Codec<T> codec, @Nullable Codec<T> networkCodec) {
        this.worldRegistries.add(new RegistryData<>(registryKey, codec, networkCodec));
    }

    public <T> void worldRegistry(Consumer<DatapackRegistryBuilder<T>> builderConsumer) {
        DatapackRegistryBuilder<T> builder = new DatapackRegistryBuilder<>(false);
        builderConsumer.accept(builder);
        this.worldRegistries.add(builder.build());
    }

    public <T> void reloadableRegistry(ResourceKey<Registry<T>> registryKey, Codec<T> codec) {
        this.reloadableRegistries.add(new RegistryData<>(registryKey, codec, null));
    }

    public <T> void reloadableRegistry(Consumer<DatapackRegistryBuilder<T>> builderConsumer) {
        DatapackRegistryBuilder<T> builder = new DatapackRegistryBuilder<>(true);
        builderConsumer.accept(builder);
        this.reloadableRegistries.add(builder.build());
    }

    /**
     * @apiNote Mekanism shim: Called by the loader after firing this event.
     */
    public void process() {
        for (RegistryData<?> registryData : this.worldRegistries) {
            register(registryData, false);
        }
        for (RegistryData<?> registryData : this.reloadableRegistries) {
            register(registryData, true);
        }
    }

    private static <T> void register(RegistryData<T> data, boolean reloadable) {
        ShimServices.BACKEND.addDatapackRegistry(data.key(), data.codec(), reloadable ? null : data.networkCodec(), reloadable);
    }

    record RegistryData<T>(ResourceKey<Registry<T>> key, Codec<T> codec, @Nullable Codec<T> networkCodec) {
    }
}
