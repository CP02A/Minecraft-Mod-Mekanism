package mekanism.fabric.shim.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Added to {@link BlockStateModel} by {@code BlockStateModelMixin}: makes Fabric's renderer collect the parts of block state models with NeoForge's level
 * aware {@code BlockStateModelExtension#collectParts(BlockAndTintGetter, BlockPos, BlockState, RandomSource, List)}, so that models can use the level and
 * the block entity's model data. Otherwise identical to {@link FabricBlockStateModel}'s default implementation.
 */
public interface LevelAwareBlockStateModel extends FabricBlockStateModel {

    @Override
    default void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
          Predicate<@Nullable Direction> cullTest) {
        boolean forceOpaque = ModelBlockRenderer.forceOpaque(Minecraft.getInstance().options.cutoutLeaves().get(), state);
        if (forceOpaque) {
            emitter.pushTransform(quad -> {
                quad.chunkLayer(net.minecraft.client.renderer.chunk.ChunkSectionLayer.SOLID);
                return true;
            });
        }
        List<BlockStateModelPart> parts = new ArrayList<>();
        ((BlockStateModel) this).collectParts(level, pos, state, random, parts);
        for (BlockStateModelPart part : parts) {
            part.emitQuads(emitter, cullTest);
        }
        if (forceOpaque) {
            emitter.popTransform();
        }
    }
}
