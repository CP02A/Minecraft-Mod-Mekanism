/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link Particle}, injected as an interface.
 */
public interface IParticleExtension {

    default Vec3 getPos() {
        Particle self = (Particle) this;
        return new Vec3(self.x, self.y, self.z);
    }
}
