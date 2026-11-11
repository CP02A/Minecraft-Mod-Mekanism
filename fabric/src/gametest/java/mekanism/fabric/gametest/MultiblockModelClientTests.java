package mekanism.fabric.gametest;

import java.util.ArrayList;
import java.util.List;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.block.attribute.AttributeHasBounding;
import mekanism.common.util.EnumUtils;
import mekanism.generators.client.model.GeneratorsModelCache;
import mekanism.generators.client.render.RenderWindGenerator;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * Checks that the baked models of Mekanism's multiblock machines (blocks with bounding blocks, such as the wind generator, the advanced solar generator or
 * the digital miner) are where the machine is: each model has to stay inside the blocks the machine occupies, and reach up into its top row. Many of these
 * models rely on NeoForge model features (composite and OBJ models, root transforms), so a feature that is lost on Fabric shows up as a model that is
 * shifted or cut off. Also checks that the wind generator's blades (rendered by its block entity renderer) sit on the top of its tower.
 */
public class MultiblockModelClientTests implements FabricClientGameTest {

    /**
     * How far (in blocks) a model may stick out of the space of its machine, models commonly have tiny overhangs (z-fighting offsets for ports for example).
     */
    private static final double TOLERANCE = 1 / 16.0;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            context.runOnClient(client -> {
                List<String> failures = new ArrayList<>();
                int checked = 0;
                for (Block block : BuiltInRegistries.BLOCK) {
                    Identifier name = BuiltInRegistries.BLOCK.getKey(block);
                    AttributeHasBounding bounding = Attribute.get(block, AttributeHasBounding.class);
                    if (bounding == null || !name.getNamespace().startsWith("mekanism")) {
                        continue;
                    }
                    for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                        checkMachine(client, name, state, bounding, failures);
                        checked++;
                    }
                }
                if (checked == 0) {
                    failures.add("No multiblock machines were found");
                }
                checkWindGeneratorRotor(client, failures);
                if (!failures.isEmpty()) {
                    throw new AssertionError(failures.size() + " multiblock model(s) render incorrectly:\n  " + String.join("\n  ", failures));
                }
            });
        }
    }

    private static void checkMachine(Minecraft client, Identifier name, BlockState state, AttributeHasBounding bounding, List<String> failures) {
        ClientLevel level = client.level;
        BlockPos origin = BlockPos.ZERO;
        //The space the machine occupies, relative to its main block
        AABB[] space = {new AABB(origin)};
        bounding.handle(level, origin, state, null, (_, pos, _) -> {
            space[0] = space[0].minmax(new AABB(pos.immutable()));
            return true;
        });
        BlockStateModel model = client.getModelManager().getBlockStateModelSet().get(state);
        if (model.particleMaterial().sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            failures.add(name + " " + state + ": the model is missing");
            return;
        }
        AABB bounds = quadBounds(collectQuads(level, origin, state, model), null);
        if (bounds == null) {
            failures.add(name + " " + state + ": the model has no quads");
            return;
        }
        AABB allowed = space[0].inflate(TOLERANCE);
        if (!contains(allowed, bounds)) {
            failures.add(name + " " + state + ": the model " + format(bounds) + " sticks out of the machine's space " + format(space[0]));
        }
        //The model has to reach into the top row of the machine (for example the wind generator's rotor, or the advanced solar generator's panels)
        if (bounds.maxY < space[0].maxY - 1 + TOLERANCE) {
            failures.add(name + " " + state + ": the model " + format(bounds) + " doesn't reach the top of the machine's space " + format(space[0]));
        }
    }

    /**
     * The wind generator's blades are rendered by {@link RenderWindGenerator} around {@link RenderWindGenerator#BLADE_OFFSET}, which has to be at the
     * rotor housing at the top of the tower (the block model).
     */
    private static void checkWindGeneratorRotor(Minecraft client, List<String> failures) {
        ClientLevel level = client.level;
        Block windGenerator = BuiltInRegistries.BLOCK.getValue(Identifier.parse("mekanismgenerators:wind_generator"));
        Vec3 hub = RenderWindGenerator.BLADE_OFFSET;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState state = Attribute.setFacing(windGenerator.defaultBlockState(), direction);
            BlockStateModel model = client.getModelManager().getBlockStateModelSet().get(state);
            //The rotor housing: the part of the tower at the height of the hub
            AABB housing = quadBounds(collectQuads(level, BlockPos.ZERO, state, model), new AABB(-1, hub.y - 0.5, -1, 2, hub.y + 0.5, 2));
            if (housing == null) {
                failures.add("wind generator facing " + direction + ": the tower doesn't reach the rotor at height " + hub.y);
            } else if (hub.x < housing.minX || hub.x > housing.maxX || hub.z < housing.minZ || hub.z > housing.maxZ) {
                failures.add("wind generator facing " + direction + ": the blades' hub " + hub + " isn't on the rotor housing " + format(housing));
            }
        }
        List<BakedQuad> blades = new ArrayList<>();
        for (BlockStateModelPart part : GeneratorsModelCache.INSTANCE.WIND_GENERATOR_BLADES.getBakedModel()) {
            addQuads(part, blades);
        }
        AABB bladeBounds = quadBounds(blades, null);
        if (bladeBounds == null) {
            failures.add("wind generator: the blades model has no quads");
        } else if (!bladeBounds.contains(0, 0, bladeBounds.getCenter().z)) {
            //The blades rotate around the model's origin, so they have to be centered on it
            failures.add("wind generator: the blades " + format(bladeBounds) + " aren't centered on their rotation axis");
        }
    }

    private static List<BakedQuad> collectQuads(ClientLevel level, BlockPos pos, BlockState state, BlockStateModel model) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(level, pos, state, level.getRandom(), parts);
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            addQuads(part, quads);
        }
        return quads;
    }

    private static void addQuads(BlockStateModelPart part, List<BakedQuad> quads) {
        for (Direction direction : EnumUtils.DIRECTIONS) {
            quads.addAll(part.getQuads(direction));
        }
        quads.addAll(part.getQuads(null));
    }

    /**
     * {@return the bounds of the vertices of the given quads, optionally only those inside the given filter}
     */
    @Nullable
    private static AABB quadBounds(List<BakedQuad> quads, @Nullable AABB filter) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        boolean found = false;
        for (BakedQuad quad : quads) {
            for (Vector3fc vertex : new Vector3fc[]{quad.position0(), quad.position1(), quad.position2(), quad.position3()}) {
                if (filter == null || filter.contains(vertex.x(), vertex.y(), vertex.z())) {
                    found = true;
                    minX = Math.min(minX, vertex.x());
                    minY = Math.min(minY, vertex.y());
                    minZ = Math.min(minZ, vertex.z());
                    maxX = Math.max(maxX, vertex.x());
                    maxY = Math.max(maxY, vertex.y());
                    maxZ = Math.max(maxZ, vertex.z());
                }
            }
        }
        return found ? new AABB(minX, minY, minZ, maxX, maxY, maxZ) : null;
    }

    private static boolean contains(AABB outer, AABB inner) {
        return inner.minX >= outer.minX && inner.minY >= outer.minY && inner.minZ >= outer.minZ
               && inner.maxX <= outer.maxX && inner.maxY <= outer.maxY && inner.maxZ <= outer.maxZ;
    }

    private static String format(AABB box) {
        return String.format("[%.3f, %.3f, %.3f -> %.3f, %.3f, %.3f]", box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
}
