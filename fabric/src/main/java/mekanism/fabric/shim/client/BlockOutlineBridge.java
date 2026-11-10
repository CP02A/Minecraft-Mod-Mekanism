package mekanism.fabric.shim.client;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import org.jspecify.annotations.Nullable;

/**
 * NeoForge stores custom block outline renderers in {@link BlockOutlineRenderState}. As the vanilla record can't hold them, they are tracked here by identity
 * for the outline states created during the current frame's extraction, and rendered from Fabric's {@link LevelRenderEvents#BEFORE_BLOCK_OUTLINE}.
 */
public final class BlockOutlineBridge {

    //Note: Extraction and rendering both happen on the render thread
    private static final Map<BlockOutlineRenderState, List<CustomBlockOutlineRenderer>> RENDERERS = new IdentityHashMap<>();
    private static List<CustomBlockOutlineRenderer> pending = List.of();

    private BlockOutlineBridge() {
    }

    public static void register() {
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, state) -> {
            boolean cancel = false;
            for (CustomBlockOutlineRenderer renderer : getRenderers(state)) {
                cancel |= renderer.render(state, context.submitNodeCollector(), context.poseStack(), context.levelState());
            }
            return !cancel;
        });
    }

    /**
     * Called at the start of extracting the block outline, the outline states of the previous frame have been rendered by now.
     */
    public static void startFrame() {
        RENDERERS.clear();
        pending = List.of();
    }

    public static void setPending(List<CustomBlockOutlineRenderer> renderers) {
        pending = renderers;
    }

    public static void attachPending(@Nullable BlockOutlineRenderState state) {
        if (state != null) {
            setRenderers(state, pending);
        }
        pending = List.of();
    }

    public static void setRenderers(BlockOutlineRenderState state, List<CustomBlockOutlineRenderer> renderers) {
        if (!renderers.isEmpty()) {
            RENDERERS.put(state, List.copyOf(renderers));
        }
    }

    public static List<CustomBlockOutlineRenderer> getRenderers(BlockOutlineRenderState state) {
        return RENDERERS.getOrDefault(state, List.of());
    }
}
