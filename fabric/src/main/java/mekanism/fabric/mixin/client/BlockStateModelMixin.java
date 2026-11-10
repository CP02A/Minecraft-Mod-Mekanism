package mekanism.fabric.mixin.client;

import mekanism.fabric.shim.client.LevelAwareBlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes Fabric's renderer pass the level to NeoForge's level aware {@code collectParts} (model data), see {@link LevelAwareBlockStateModel}.
 */
@Mixin(BlockStateModel.class)
public interface BlockStateModelMixin extends LevelAwareBlockStateModel {
}
