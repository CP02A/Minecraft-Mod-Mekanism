package mekanism.common.config;

import java.util.Locale;
import net.neoforged.fml.config.ModConfig;

/// Loader agnostic type of a Mekanism config.
///
/// NeoForge has dedicated config types for configs that are synced from the server, and for configs that are local to each instance, which other loaders'
/// config libraries may not have. In that case the closest equivalent type is used instead.
public enum MekanismConfigType {
    /// Client only config.
    CLIENT("CLIENT"),
    /// Config that is local to each instance, and not synced.
    LOCAL("COMMON"),
    /// Config that is controlled by the server and synced to clients.
    SYNCED("SERVER"),
    /// Config that is loaded during startup, before registration.
    STARTUP("STARTUP");

    private final String fallback;

    MekanismConfigType(String fallback) {
        this.fallback = fallback;
    }

    /// {@return the loader's config type that corresponds to this type}
    public ModConfig.Type toLoaderType() {
        try {
            return ModConfig.Type.valueOf(name());
        } catch (IllegalArgumentException e) {
            return ModConfig.Type.valueOf(fallback.toUpperCase(Locale.ROOT));
        }
    }
}
