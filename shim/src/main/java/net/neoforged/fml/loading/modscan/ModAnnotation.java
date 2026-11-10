/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.modscan;

/**
 * Mekanism shim: Values used in annotation scan data.
 */
public class ModAnnotation {

    /**
     * An enum constant referenced in an annotation.
     *
     * @param desc  The descriptor of the enum class.
     * @param value The name of the enum constant.
     */
    public record EnumHolder(String desc, String value) {
    }
}
