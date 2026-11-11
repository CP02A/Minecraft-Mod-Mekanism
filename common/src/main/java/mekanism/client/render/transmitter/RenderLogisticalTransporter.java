package mekanism.client.render.transmitter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import mekanism.api.text.EnumColor;
import mekanism.client.model.MekanismModelCache;
import mekanism.client.render.transmitter.TransmitterRenderState.TransporterRenderState;
import mekanism.client.render.transmitter.TransmitterRenderState.TransporterRenderState.TransporterStackRenderState;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterStack;
import mekanism.common.tile.transmitter.TileEntityLogisticalTransporterBase;
import mekanism.common.util.TransporterUtils;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class RenderLogisticalTransporter extends RenderTransmitterBase<TileEntityLogisticalTransporterBase, TransporterRenderState> {

    private static final float FLAT_ITEM_DEPTH_THRESHOLD = 0.0625F;
    private static final float SPREAD = 0.15F;

    private final ItemModelResolver itemModelResolver;
    private final RandomSource random = RandomSource.create();

    public RenderLogisticalTransporter(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public TransporterRenderState createRenderState() {
        return new TransporterRenderState();
    }

    @Override
    public void extractRenderState(TileEntityLogisticalTransporterBase transporter, TransporterRenderState state, float partialTick, Vec3 cameraPosition,
          ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(transporter, state, partialTick, cameraPosition, breakProgress);
        LogisticalTransporterBase transmitter = transporter.getTransmitter();
        Collection<TransporterStack> inTransit = transmitter.getTransit();
        state.stacks = Collections.emptyList();
        if (!inTransit.isEmpty()) {
            Level level = transporter.getLevel();
            float partial = partialTick * transmitter.tier.getSpeed();
            record TransportInformation(int progress, ItemResource item, @Nullable EnumColor color) {
            }
            Map<TransportInformation, TransporterStackRenderState> information = new Object2ObjectLinkedOpenHashMap<>(inTransit.size());
            for (TransporterStack stack : inTransit) {
                if (stack.isEmpty()) {
                    continue;
                }
                //Shrink the in transit list as much as possible. Don't render things of the same type that are in the same spot with the same color twice.
                // Note: We don't sum the sizes, as stacks only briefly share a spot (for example while waiting at the end of a transporter for the server
                // to move them along), and summing would make the rendered cluster grow and then shrink back down
                TransportInformation key = new TransportInformation(stack.progress, stack.getItemType(), stack.color);
                TransporterStackRenderState existing = information.get(key);
                if (existing != null) {
                    existing.itemCount = Math.max(existing.itemCount, stack.size());
                    continue;
                }
                Vector3f stackPos = TransporterUtils.getStackPosition(transmitter, stack, partial);
                //Similar to campfire renderer's seed, except we bind it to the original location so that when the stack goes from one transporter to the next
                // it doesn't change types
                int seed = (int) stack.originalLocation;
                TransporterStackRenderState stackRenderState = new TransporterStackRenderState(stackPos, stack.getSide(transmitter).getAxis(), stack.color, seed, stack.size());
                this.itemModelResolver.updateForTopItem(stackRenderState.item, stack.asItemStack(), ItemDisplayContext.NONE, level, null, seed);
                information.put(key, stackRenderState);
            }
            state.stacks = new ArrayList<>(information.values());
        }
    }

    @Override
    public void submit(TransporterRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState camera) {
        //Note: We expect this to not actually render anything, but call it just in case for consistency
        super.submit(state, poseStack, nodeCollector, camera);
        if (!state.stacks.isEmpty()) {
            for (TransporterStackRenderState stackRenderState : state.stacks) {
                Vector3f stackPos = stackRenderState.stackPos;
                poseStack.pushPose();
                poseStack.translate(stackPos.x(), stackPos.y(), stackPos.z());
                if (stackRenderState.color != null) {
                    poseStack.pushPose();
                    poseStack.translate(-0.5F, -0.25F, -0.5F);
                    nodeCollector.submitBlockModel(poseStack, Sheets.cutoutBlockItemSheet(), MekanismModelCache.INSTANCE.TRANSPORTER_BOX.getBakedModel(),
                          new int[]{stackRenderState.color.getPackedColor()}, state.lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
                    poseStack.popPose();
                }
                //Turn the item so that it faces the camera, snapped to a horizontal direction
                double dx = camera.pos.x - (state.blockPos.getX() + stackPos.x());
                double dz = camera.pos.z - (state.blockPos.getZ() + stackPos.z());
                //Note: Only the positions get turned, not the normals, so that the item gets lit the same no matter which way it faces. Otherwise, items
                // facing east or west are much darker than ones facing north or south, due to the direction of the item lighting
                Matrix3f normal = new Matrix3f(poseStack.last().normal());
                //Items moving horizontally face across the transporter, towards the side the camera is on. This way they only flip around when the camera
                // crosses the line the item is moving along, where the item is seen edge on anyway (if we picked whichever axis points the most towards the
                // camera, items next to each other would face different ways when looking at the transporter diagonally)
                boolean faceX = switch (stackRenderState.travelAxis) {
                    case X -> false;
                    case Z -> true;
                    case Y -> Math.abs(dx) > Math.abs(dz);
                };
                if (faceX) {
                    poseStack.rotate(Axis.YP, dx > 0 ? Mth.HALF_PI : -Mth.HALF_PI);
                } else if (dz < 0) {
                    poseStack.rotate(Axis.YP, Mth.PI);
                }
                poseStack.last().normal().set(normal);
                AABB bb = stackRenderState.item.getModelBoundingBox();
                double maxDimension = Math.max(bb.getXsize(), Math.max(bb.getYsize(), bb.getZsize()));
                //TODO: Do we want to scale up tiny models?
                if (maxDimension > 1) {
                    //Scale any overly large models down to a single unit
                    float scale = (float) (1 / maxDimension);
                    poseStack.scale(scale, scale, scale);
                    poseStack.translate(0, 0.25F, 0);
                }
                poseStack.translate(0, 0.25F, 0);
                poseStack.scale(0.25F, 0.25F, 0.25F);
                submitCluster(stackRenderState, (float) bb.getZsize(), poseStack, nodeCollector, state.lightCoords);
                poseStack.popPose();
            }
        }
    }

    /// Renders multiple copies of the item depending on how many items the stack contains, similar to how dropped item entities are rendered.
    private void submitCluster(TransporterStackRenderState stackRenderState, float modelDepth, PoseStack poseStack, SubmitNodeCollector nodeCollector, int lightCoords) {
        ItemStackRenderState item = stackRenderState.item;
        int amount = ItemClusterRenderState.getRenderedAmount(stackRenderState.itemCount);
        random.setSeed(stackRenderState.seed);
        if (modelDepth > FLAT_ITEM_DEPTH_THRESHOLD) {
            //3D model: jitter the copies around the center
            item.submit(poseStack, nodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
            for (int i = 1; i < amount; i++) {
                poseStack.pushPose();
                poseStack.translate(jitter(SPREAD), jitter(SPREAD), jitter(SPREAD));
                item.submit(poseStack, nodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
                poseStack.popPose();
            }
        } else {
            //Flat model: layer the copies on top of each other, slightly offset, like dropped items do.
            //Note: Dropped items stack the copies towards +z, which is the back of flat item models (the side the camera sees is -z). As we always show the
            // front of the item, we stack them towards -z instead so that the offset copies end up on top of the first one rather than peeking out from behind it
            float offsetZ = -modelDepth * 1.5F;
            poseStack.pushPose();
            poseStack.translate(0, 0, -(offsetZ * (amount - 1) / 2F));
            item.submit(poseStack, nodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
            poseStack.translate(0, 0, offsetZ);
            for (int i = 1; i < amount; i++) {
                poseStack.pushPose();
                poseStack.translate(jitter(SPREAD / 2), jitter(SPREAD / 2), 0);
                item.submit(poseStack, nodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, EntityRenderState.NO_OUTLINE);
                poseStack.popPose();
                poseStack.translate(0, 0, offsetZ);
            }
            poseStack.popPose();
        }
    }

    private float jitter(float spread) {
        return (random.nextFloat() * 2 - 1) * spread;
    }

}