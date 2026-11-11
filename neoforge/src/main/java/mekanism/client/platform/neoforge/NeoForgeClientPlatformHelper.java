package mekanism.client.platform.neoforge;

import net.neoforged.neoforge.client.model.quad.BakedNormals;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import mekanism.client.platform.IClientPlatformHelper;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
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
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.util.CommonColors;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import java.util.Map;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.Hud;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.modlist.ModDisplayInfo;

public class NeoForgeClientPlatformHelper implements IClientPlatformHelper {

    @Override
    public int getHudLeftHeight(Hud hud) {
        return hud.leftHeight;
    }

    @Override
    public int getHudRightHeight(Hud hud) {
        return hud.rightHeight;
    }

    @Override
    public void addHudLeftHeight(Hud hud, int height) {
        hud.leftHeight += height;
    }

    @Override
    public void registerClientExtensions(ModContainer container, Supplier<Component> modName) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.<ModDisplayInfo>registerExtensionPoint(ModDisplayInfo.class, () -> new MekanismModDisplayInfo(container, modName));
    }

    @Override
    public BlockStateModelPart bakeSimpleModel(ModelBaker baker, ResolvedModel model, ModelState state) {
        return SimpleModelWrapper.bake(baker, model, state);
    }

    @Override
    public int getFluidTint(FluidModel fluidModel, Fluid fluid) {
        FluidTintSource tintSource = fluidModel.fluidTintSource();
        return tintSource == null ? CommonColors.WHITE : tintSource.color(fluid.defaultFluidState());
    }

    @Override
    public float getPartialTick(EntityRenderState state) {
        return state.partialTick;
    }

    @Override
    public BlockOutlineRenderState createOutlineState(BlockPos pos, boolean translucent, boolean highContrast, VoxelShape shape, @Nullable VoxelShape collisionShape,
          @Nullable VoxelShape occlusionShape, @Nullable VoxelShape interactionShape, List<CustomBlockOutlineRenderer> customRenderers) {
        return new BlockOutlineRenderState(pos, translucent, highContrast, shape, collisionShape, occlusionShape, interactionShape, customRenderers);
    }

    @Override
    public List<CustomBlockOutlineRenderer> getCustomOutlineRenderers(BlockOutlineRenderState state) {
        return state.customRenderers();
    }

    @Override
    public CuboidModelElement copyCuboidElement(CuboidModelElement element, Map<Direction, CuboidFace> faces, @Nullable Direction shadeDirectionOverride, int lightEmission) {
        return new CuboidModelElement(element.from(), element.to(), faces, element.rotation(), shadeDirectionOverride, lightEmission, element.faceData());
    }

    @Override
    public CuboidFace copyCuboidFace(CuboidFace face, CuboidFace.UVs uvs) {
        return new CuboidFace(face.cullForDirection(), face.tintIndex(), face.texture(), uvs, face.rotation(), face.faceData(), face.parent());
    }

    @Override
    public BakedQuad flipQuad(BakedQuad quad) {
        BakedColors colors = quad.bakedColors();
        //Note: Normals are left unspecified so that they are derived from the flipped direction
        return new BakedQuad(quad.position0(), quad.position3(), quad.position2(), quad.position1(),
              quad.packedUV0(), quad.packedUV3(), quad.packedUV2(), quad.packedUV1(), quad.direction().getOpposite(), quad.materialInfo(),
              BakedNormals.UNSPECIFIED, BakedColors.of(colors.color(0), colors.color(3), colors.color(2), colors.color(1)));
    }
}
