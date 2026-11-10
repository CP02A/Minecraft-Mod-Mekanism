package mekanism.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.color.block.BlockColors;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * NeoForge's {@link RegisterColorHandlersEvent.BlockTintSources}.
 */
@Mixin(BlockColors.class)
public abstract class BlockColorsMixin {

    @ModifyReturnValue(method = "createDefault", at = @At("RETURN"))
    private static BlockColors mekanism$registerTintSources(BlockColors colors) {
        ModLoader.postEvent(new RegisterColorHandlersEvent.BlockTintSources(colors));
        return colors;
    }
}
