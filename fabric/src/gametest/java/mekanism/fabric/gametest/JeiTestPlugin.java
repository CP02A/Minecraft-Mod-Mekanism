package mekanism.fabric.gametest;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Captures JEI's runtime for {@link JeiClientTests} (registered through the {@code jei_mod_plugin} entrypoint, so only loaded with JEI).
 */
public class JeiTestPlugin implements IModPlugin {

    @Nullable
    static volatile IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("mekanism_fabric_tests", "jei_test_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }
}
