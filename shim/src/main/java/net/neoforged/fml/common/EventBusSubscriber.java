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
 * Marks a class whose static {@link net.neoforged.bus.api.SubscribeEvent} methods should be automatically registered.
 *
 * <p>Mekanism shim: Registered by the loader from the annotation scan data.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventBusSubscriber {

    Dist[] value() default { Dist.CLIENT, Dist.DEDICATED_SERVER };

    String modid() default "";
}
