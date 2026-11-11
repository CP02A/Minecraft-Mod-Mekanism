package mekanism.client.platform;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import java.util.List;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;
import org.jspecify.annotations.Nullable;
import java.util.Map;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.Hud;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;

/**
 * Exposes client side functionality that differs between mod loaders.
 *
 * @implNote Each loader specific project provides an implementation of this via a {@link java.util.ServiceLoader} service file.
 */
public interface IClientPlatformHelper {

    /**
     * {@return the height of the HUD elements stacked on the left side above the hotbar}
     */
    int getHudLeftHeight(Hud hud);

    /**
     * {@return the height of the HUD elements stacked on the right side above the hotbar}
     */
    int getHudRightHeight(Hud hud);

    /**
     * Reserves extra height on the left side above the hotbar, so that other HUD elements render above it.
     */
    void addHudLeftHeight(Hud hud, int height);

    /**
     * Registers loader specific extension points of the mod container, such as the config screen and mod list information.
     */
    void registerClientExtensions(ModContainer container, Supplier<Component> modName);

    /**
     * Bakes a simple model from an already resolved model.
     */
    BlockStateModelPart bakeSimpleModel(ModelBaker baker, ResolvedModel model, ModelState state);

    /**
     * {@return the tint color of the fluid model for the given fluid}
     */
    int getFluidTint(FluidModel fluidModel, Fluid fluid);

    /**
     * {@return the partial tick the render state was extracted with}
     */
    float getPartialTick(EntityRenderState state);

    /**
     * Creates a block outline render state, with the given custom outline renderers where supported.
     */
    BlockOutlineRenderState createOutlineState(BlockPos pos, boolean translucent, boolean highContrast, VoxelShape shape, @Nullable VoxelShape collisionShape,
          @Nullable VoxelShape occlusionShape, @Nullable VoxelShape interactionShape, List<CustomBlockOutlineRenderer> customRenderers);

    /**
     * {@return the custom outline renderers of the outline render state}
     */
    List<CustomBlockOutlineRenderer> getCustomOutlineRenderers(BlockOutlineRenderState state);

    /**
     * {@return a copy of the element with the given faces, shade direction override and light emission, keeping any loader specific data}
     */
    CuboidModelElement copyCuboidElement(CuboidModelElement element, Map<Direction, CuboidFace> faces, @Nullable Direction shadeDirectionOverride, int lightEmission);

    /**
     * {@return a copy of the face with the given uvs, keeping any loader specific data}
     */
    CuboidFace copyCuboidFace(CuboidFace face, CuboidFace.UVs uvs);

    /**
     * {@return a copy of the quad facing the opposite way (reversed winding and direction), keeping any loader specific data}
     */
    BakedQuad flipQuad(BakedQuad quad);
}
