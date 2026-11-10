/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.shim.ShimHooks;

/**
 * Mekanism shim: NeoForge's key mapping conflict contexts and modifiers, injected into {@link KeyMapping}.
 *
 * @apiNote Contexts and modifiers are stored by the shim, but vanilla's key handling does not take them into account.
 */
public interface IKeyMappingExtension {

    private KeyMapping self() {
        return (KeyMapping) this;
    }

    default InputConstants.Key getKey() {
        return self().key;
    }

    default boolean isActiveAndMatches(InputConstants.Key keyCode) {
        return keyCode != InputConstants.UNKNOWN && keyCode.equals(getKey()) && getKeyConflictContext().isActive() && getKeyModifier().isActive(getKeyConflictContext());
    }

    default void setToDefault() {
        setKeyModifierAndCode(getDefaultKeyModifier(), self().getDefaultKey());
    }

    default void setKeyConflictContext(IKeyConflictContext keyConflictContext) {
        ShimHooks.getKeyMappingData(self()).conflictContext = keyConflictContext;
    }

    default IKeyConflictContext getKeyConflictContext() {
        IKeyConflictContext context = (IKeyConflictContext) ShimHooks.getKeyMappingData(self()).conflictContext;
        return context == null ? KeyConflictContext.UNIVERSAL : context;
    }

    default KeyModifier getDefaultKeyModifier() {
        return KeyModifier.NONE;
    }

    default KeyModifier getKeyModifier() {
        KeyModifier modifier = (KeyModifier) ShimHooks.getKeyMappingData(self()).modifier;
        return modifier == null ? KeyModifier.NONE : modifier;
    }

    default void setKeyModifierAndCode(KeyModifier keyModifier, InputConstants.Key keyCode) {
        ShimHooks.getKeyMappingData(self()).modifier = keyModifier;
        self().setKey(keyCode);
    }

    default boolean isConflictContextAndModifierActive() {
        return getKeyConflictContext().isActive() && getKeyModifier().isActive(getKeyConflictContext());
    }
}
