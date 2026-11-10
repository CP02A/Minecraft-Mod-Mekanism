/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.neoforged.api.distmarker.Dist;

/**
 * Marks a mod entrypoint class.
 *
 * <p>Mekanism shim: The loader specific entrypoint constructs these classes itself.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {

    String value();

    Dist[] dist() default { Dist.CLIENT, Dist.DEDICATED_SERVER };
}
