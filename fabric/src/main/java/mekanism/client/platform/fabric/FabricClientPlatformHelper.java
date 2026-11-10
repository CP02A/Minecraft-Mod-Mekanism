package mekanism.client.platform.fabric;

import mekanism.client.platform.IClientPlatformHelper;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.CommonColors;
import java.util.Map;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.Hud;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;

public class FabricClientPlatformHelper implements IClientPlatformHelper {

    /**
     * Vanilla's height of the health and armor rows above the hotbar.
     */
    private static final int VANILLA_LEFT_HEIGHT = 49;
    /**
     * Vanilla's height of the food and air rows above the hotbar.
     */
    private static final int VANILLA_RIGHT_HEIGHT = 49;

    @Override
    public int getHudLeftHeight(Hud hud) {
        return VANILLA_LEFT_HEIGHT;
    }

    @Override
    public int getHudRightHeight(Hud hud) {
        return VANILLA_RIGHT_HEIGHT;
    }

    @Override
    public void addHudLeftHeight(Hud hud, int height) {
        //Fabric does not track the stacking of HUD elements
    }

    @Override
    public void registerClientExtensions(ModContainer container, Supplier<Component> modName) {
        //Config screens are provided through Mod Menu's integration on Fabric
    }

    @Override
    public BlockStateModelPart bakeSimpleModel(ModelBaker baker, ResolvedModel model, ModelState state) {
        //Same as vanilla's SimpleModelWrapper#bake, but for a model that has already been resolved
        TextureSlots textureSlots = model.getTopTextureSlots();
        Material.Baked particleMaterial = model.resolveParticleMaterial(textureSlots, baker);
        QuadCollection geometry = model.bakeTopGeometry(textureSlots, baker, state);
        return new SimpleModelWrapper(geometry, model.getTopAmbientOcclusion(), particleMaterial);
    }

    @Override
    public int getFluidTint(FluidModel fluidModel, Fluid fluid) {
        BlockTintSource tintSource = fluidModel.tintSource();
        return tintSource == null ? CommonColors.WHITE : tintSource.color(fluid.defaultFluidState().createLegacyBlock());
    }

    @Override
    public float getPartialTick(EntityRenderState state) {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
    }

    @Override
    public BlockOutlineRenderState createOutlineState(BlockPos pos, boolean translucent, boolean highContrast, VoxelShape shape, @Nullable VoxelShape collisionShape,
          @Nullable VoxelShape occlusionShape, @Nullable VoxelShape interactionShape, List<CustomBlockOutlineRenderer> customRenderers) {
        //Custom outline renderers are tracked by the Fabric outline integration instead of the render state
        return new BlockOutlineRenderState(pos, translucent, highContrast, shape, collisionShape, occlusionShape, interactionShape);
    }

    @Override
    public List<CustomBlockOutlineRenderer> getCustomOutlineRenderers(BlockOutlineRenderState state) {
        return List.of();
    }

    @Override
    public CuboidModelElement copyCuboidElement(CuboidModelElement element, Map<Direction, CuboidFace> faces, @Nullable Direction shadeDirectionOverride, int lightEmission) {
        return new CuboidModelElement(element.from(), element.to(), faces, element.rotation(), shadeDirectionOverride, lightEmission);
    }

    @Override
    public CuboidFace copyCuboidFace(CuboidFace face, CuboidFace.UVs uvs) {
        return new CuboidFace(face.cullForDirection(), face.tintIndex(), face.texture(), uvs, face.rotation());
    }
}
