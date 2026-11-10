/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Mekanism shim: FML's inter-mod communication. Messages are only visible to mods that use the shim.
 */
public class InterModComms {

    private static final Map<String, List<IMCMessage>> MESSAGES = new ConcurrentHashMap<>();

    public static boolean sendTo(String modId, String method, Supplier<?> thing) {
        return sendTo("unknown", modId, method, thing);
    }

    public static boolean sendTo(String senderModId, String modId, String method, Supplier<?> thing) {
        MESSAGES.computeIfAbsent(modId, k -> new CopyOnWriteArrayList<>()).add(new IMCMessage(senderModId, modId, method, thing));
        return true;
    }

    public static Stream<IMCMessage> getMessages(String modId, Predicate<String> methodMatcher) {
        List<IMCMessage> messages = MESSAGES.remove(modId);
        return messages == null ? Stream.empty() : messages.stream().filter(message -> methodMatcher.test(message.method()));
    }

    public static Stream<IMCMessage> getMessages(String modId) {
        return getMessages(modId, method -> true);
    }

    public record IMCMessage(String senderModId, String modId, String method, Supplier<?> messageSupplier) {

        @SuppressWarnings("unchecked")
        public <T> Supplier<T> getMessageSupplier() {
            return (Supplier<T>) messageSupplier;
        }
    }
}
