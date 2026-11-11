package mekanism.fabric.gametest;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import mcjty.theoneprobe.TheOneProbe;
import mcjty.theoneprobe.api.IElement;
import mcjty.theoneprobe.api.IProbeConfig;
import mcjty.theoneprobe.api.IProbeConfig.ConfigMode;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.apiimpl.ProbeInfo;
import mcjty.theoneprobe.apiimpl.elements.AbstractElementPanel;
import mcjty.theoneprobe.network.PacketGetInfo;
import mcp.mobius.waila.access.ServerAccessor;
import mcp.mobius.waila.api.IData;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IInstanceRegistry;
import mcp.mobius.waila.config.PluginConfig;
import mcp.mobius.waila.registry.Registrar;
import mekanism.api.AutomationType;
import mekanism.api.MekanismRegistries;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.ChemicalIds;
import mekanism.api.chemical.ChemicalResource;
import mekanism.common.command.builders.StructureBuilder;
import mekanism.common.integration.lookingat.ChemicalElement;
import mekanism.common.integration.lookingat.EnergyElement;
import mekanism.common.integration.lookingat.FluidElement;
import mekanism.common.integration.lookingat.ILookingAtElement;
import mekanism.common.integration.lookingat.LookingAtElementType;
import mekanism.common.integration.lookingat.jade.JadeDataProvider;
import mekanism.common.integration.lookingat.theoneprobe.TOPEnergyElement;
import mekanism.common.integration.lookingat.theoneprobe.TOPFluidElement;
import mekanism.common.integration.lookingat.theoneprobe.TOPProvider;
import mekanism.common.integration.lookingat.wthit.WTHITDataProvider;
import mekanism.common.integration.lookingat.wthit.WTHITLookingAtHelper;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.TileEntityChemicalTank;
import mekanism.common.tile.TileEntityEnergyCube;
import mekanism.common.tile.TileEntityFluidTank;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.common.content.tank.TankMultiblockData;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.impl.BlockAccessorImpl;
import snownee.jade.impl.WailaCommonRegistration;
import snownee.jade.network.RequestBlockPacket;
import snownee.jade.network.ServerPayloadContext;

/**
 * Block info overlay integrations (Jade, The One Probe, WTHIT): runs the server side data providers the way the mods do when a client looks at a
 * block, and checks the energy, fluid and chemical info they sync to the client (after a network round trip).
 */
public class LookingAtTests {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static <BE extends BlockEntity> BE place(GameTestHelper helper, BlockPos pos, BlockRegistryObject<?, ?> block, Class<BE> beClass) {
        helper.setBlock(pos, block.defaultState());
        BE blockEntity = helper.getBlockEntity(pos, beClass);
        blockEntity.applyComponentsFromItemStack(new ItemStack(block));
        return blockEntity;
    }

    private static ServerPlayer player(GameTestHelper helper, BlockPos lookingAt) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(lookingAt).north(2)));
        return player;
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false);
    }

    private static EnergyCubeSetup energyCube(GameTestHelper helper) {
        TileEntityEnergyCube cube = place(helper, POS, MekanismBlocks.BASIC_ENERGY_CUBE, TileEntityEnergyCube.class);
        cube.energyContainer().setEnergy(12_345, null);
        return new EnergyCubeSetup(cube.energyContainer().getCapacityAsLong());
    }

    private record EnergyCubeSetup(long capacity) {
    }

    private static void fluidTank(GameTestHelper helper) {
        TileEntityFluidTank tank = place(helper, POS, MekanismBlocks.BASIC_FLUID_TANK, TileEntityFluidTank.class);
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(tank.fluidTank.insert(FluidResource.of(Fluids.WATER), 1_000, transaction, AutomationType.INTERNAL), 1_000, "inserted water");
            transaction.commit();
        }
    }

    private static ChemicalResource chemicalTank(GameTestHelper helper) {
        TileEntityChemicalTank tank = place(helper, POS, MekanismBlocks.BASIC_CHEMICAL_TANK, TileEntityChemicalTank.class);
        ChemicalResource hydrogen = ChemicalResource.of(helper.getLevel().registryAccess().lookupOrThrow(MekanismRegistries.Keys.CHEMICAL).getOrThrow(ChemicalIds.HYDROGEN));
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(tank.getChemicalTank().insert(hydrogen, 1_000, transaction, AutomationType.INTERNAL), 1_000, "inserted hydrogen");
            transaction.commit();
        }
        return hydrogen;
    }

    private static <E extends ILookingAtElement> E single(GameTestHelper helper, List<? extends ILookingAtElement> elements, Class<E> type) {
        List<E> matching = elements.stream().filter(type::isInstance).map(type::cast).toList();
        helper.assertValueEqual(matching.size(), 1, type.getSimpleName() + " elements in " + elements);
        return matching.getFirst();
    }

    //Jade

    /**
     * Handles a block data request like Jade's server does (the providers the client asks for are the ones registered for the block entity), and
     * decodes Mekanism's data from the response like the client side tooltip renderer.
     */
    private static List<ILookingAtElement> jadeElements(GameTestHelper helper, BlockPos pos) {
        Level level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = level.getBlockState(absolute);
        BlockEntity blockEntity = level.getBlockEntity(absolute);
        List<IServerDataProvider<BlockAccessor>> providers = WailaCommonRegistration.instance().blockDataProvidersOf(state, blockEntity, true);
        helper.assertTrue(providers.stream().anyMatch(JadeDataProvider.class::isInstance),
              Component.literal("Expected Mekanism's Jade block data provider to be registered for " + blockEntity + ", got " + providers));
        ServerPlayer player = player(helper, pos);
        CompoundTag[] response = new CompoundTag[1];
        BlockHitResult hit = hit(helper, pos);
        BlockAccessorImpl.handleRequest(new RequestBlockPacket(new BlockAccessorImpl.SyncData(true, hit, ItemStack.EMPTY, new CompoundTag()), providers),
              new ServerPayloadContext() {
                  @Override
                  public void execute(Runnable runnable) {
                      runnable.run();
                  }

                  @Override
                  public ServerPlayer player() {
                      return player;
                  }
              }, tag -> response[0] = tag);
        helper.assertTrue(response[0] != null, Component.literal("Jade's server didn't respond to the block data request"));
        BlockAccessor clientAccessor = new BlockAccessorImpl.Builder().level(level).player(player).hit(hit).blockState(state).blockEntity(() -> blockEntity)
              .serverData(response[0]).build();
        Tag mekData = clientAccessor.getServerData().get(SerializationConstants.MEK_DATA);
        helper.assertTrue(mekData != null, Component.literal("Expected Mekanism data in Jade's server data " + response[0]));
        Optional<List<ILookingAtElement>> elements = clientAccessor.decodeFromNbt(LookingAtElementType.ELEMENT_LIST_STREAM_CODEC, mekData);
        helper.assertTrue(elements.isPresent(), Component.literal("Couldn't decode Mekanism's Jade data"));
        return elements.get();
    }

    @GameTest
    public void jadeSyncsEnergyCubeEnergy(GameTestHelper helper) {
        EnergyCubeSetup cube = energyCube(helper);
        EnergyElement energy = single(helper, jadeElements(helper, POS), EnergyElement.class);
        helper.assertValueEqual(energy.getEnergy(), 12_345L, "energy synced by Jade");
        helper.assertValueEqual(energy.getMaxEnergy(), cube.capacity(), "max energy synced by Jade");
        helper.succeed();
    }

    @GameTest
    public void jadeSyncsFluidTankFluid(GameTestHelper helper) {
        fluidTank(helper);
        FluidElement fluid = single(helper, jadeElements(helper, POS), FluidElement.class);
        helper.assertTrue(fluid.getStored().resource().is(Fluids.WATER), Component.literal("Expected water, got " + fluid.getStored()));
        helper.assertValueEqual(fluid.getStored().amount(), 1_000L, "fluid amount synced by Jade");
        helper.assertValueEqual(fluid.getCapacity(), helper.getBlockEntity(POS, TileEntityFluidTank.class).fluidTank.capacityAsLong(FluidResource.of(Fluids.WATER)), "fluid capacity synced by Jade");
        helper.succeed();
    }

    @GameTest
    public void jadeSyncsChemicalTankChemical(GameTestHelper helper) {
        ChemicalResource hydrogen = chemicalTank(helper);
        ChemicalElement chemical = single(helper, jadeElements(helper, POS), ChemicalElement.class);
        helper.assertValueEqual(chemical.getStored().resource(), hydrogen, "chemical synced by Jade");
        helper.assertValueEqual(chemical.getStored().amount(), 1_000L, "chemical amount synced by Jade");
        helper.succeed();
    }

    /**
     * Looking at a casing (not a valve) of a formed dynamic tank shows the multiblock's contents.
     */
    @GameTest(maxTicks = 200)
    public void jadeSyncsDynamicTankMultiblockFluid(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 1, 1);
        BlockPos casing = start.offset(1, 0, 0);
        new StructureBuilder(3, 3, 3) {
            @Override
            protected void build(Level world, BlockPos s, boolean empty) {
                buildFrame(world, s);
                buildWalls(world, s);
                buildInteriorLayers(world, s, 1, 1, Blocks.AIR.defaultBlockState());
            }

            @Override
            protected BlockState getWallBlock(BlockPos pos) {
                return MekanismBlocks.STRUCTURAL_GLASS.defaultState();
            }

            @Override
            protected BlockState getCasing() {
                return MekanismBlocks.DYNAMIC_TANK.defaultState();
            }
        }.build(helper.getLevel(), helper.absolutePos(start), false);
        helper.startSequence()
              .thenWaitUntil(() -> helper.assertTrue(helper.getBlockEntity(casing, TileEntityMultiblock.class).getMultiblock().isFormed(),
                    Component.literal("The dynamic tank should form")))
              .thenExecute(() -> {
                  TankMultiblockData tank = (TankMultiblockData) helper.getBlockEntity(casing, TileEntityMultiblock.class).getMultiblock();
                  try (Transaction transaction = Transaction.openRoot()) {
                      tank.getFluidTank().insert(FluidResource.of(Fluids.WATER), 5_000, transaction, AutomationType.INTERNAL);
                      transaction.commit();
                  }
                  FluidElement fluid = single(helper, jadeElements(helper, casing), FluidElement.class);
                  helper.assertTrue(fluid.getStored().resource().is(Fluids.WATER), Component.literal("Expected water, got " + fluid.getStored()));
                  helper.assertValueEqual(fluid.getStored().amount(), 5_000L, "multiblock fluid amount synced by Jade");
                  helper.assertValueEqual(fluid.getCapacity(), tank.getFluidTank().capacityAsLong(FluidResource.of(Fluids.WATER)), "multiblock fluid capacity synced by Jade");
              })
              .thenSucceed();
    }

    //The One Probe

    /**
     * Builds the probe info like The One Probe's server does when it gets a {@link PacketGetInfo} (its builder is private), and sends it through
     * the network codec.
     */
    private static List<IElement> topElements(GameTestHelper helper, BlockPos pos) {
        helper.assertTrue(TheOneProbe.theOneProbeImp.getProviders().stream().anyMatch(TOPProvider.class::isInstance),
              Component.literal("Expected Mekanism's probe info provider to be registered"));
        ServerPlayer player = player(helper, pos);
        //The extended probe mode (by default the one that shows tanks) needs a probe
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(TheOneProbe.MODID, "probe"))));
        BlockHitResult hit = hit(helper, pos);
        //Mekanism shows tanks in the probe mode of The One Probe's "show tanks" setting
        IProbeConfig defaultConfig = TheOneProbe.theOneProbeImp.createProbeConfig();
        helper.assertTrue(defaultConfig.getTankMode() > 0, Component.literal("The One Probe's tank display is disabled: " + defaultConfig.getTankMode()));
        ProbeMode mode = defaultConfig.getShowTankSetting() == ConfigMode.NORMAL ? ProbeMode.NORMAL : ProbeMode.EXTENDED;
        ProbeInfo info;
        try {
            Method getProbeInfo = PacketGetInfo.class.getDeclaredMethod("getProbeInfo", net.minecraft.world.entity.player.Player.class, ProbeMode.class, Level.class,
                  BlockPos.class, Direction.class, Vec3.class, ItemStack.class);
            getProbeInfo.setAccessible(true);
            info = (ProbeInfo) getProbeInfo.invoke(null, player, mode, helper.getLevel(), hit.getBlockPos(), hit.getDirection(), hit.getLocation(), ItemStack.EMPTY);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.assertTrue(info != null, Component.literal("Expected probe info"));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        ProbeInfo.STREAM_CODEC.encode(buffer, info);
        List<IElement> elements = new ArrayList<>();
        collect(ProbeInfo.STREAM_CODEC.decode(buffer).getElements(), elements);
        return elements;
    }

    private static void collect(List<IElement> elements, List<IElement> into) {
        for (IElement element : elements) {
            into.add(element);
            if (element instanceof AbstractElementPanel panel) {
                collect(panel.getElements(), into);
            }
        }
    }

    @GameTest
    public void theOneProbeSyncsEnergyCubeEnergy(GameTestHelper helper) {
        EnergyCubeSetup cube = energyCube(helper);
        List<IElement> elements = topElements(helper, POS);
        List<TOPEnergyElement> energy = elements.stream().filter(TOPEnergyElement.class::isInstance).map(TOPEnergyElement.class::cast).toList();
        helper.assertValueEqual(energy.size(), 1, "energy elements in " + elements);
        helper.assertValueEqual(energy.getFirst().getEnergy(), 12_345L, "energy synced by The One Probe");
        helper.assertValueEqual(energy.getFirst().getMaxEnergy(), cube.capacity(), "max energy synced by The One Probe");
        helper.succeed();
    }

    @GameTest
    public void theOneProbeSyncsFluidTankFluid(GameTestHelper helper) {
        fluidTank(helper);
        List<IElement> elements = topElements(helper, POS);
        List<TOPFluidElement> fluids = elements.stream().filter(TOPFluidElement.class::isInstance).map(TOPFluidElement.class::cast).toList();
        helper.assertValueEqual(fluids.size(), 1, "fluid elements in " + elements);
        helper.assertTrue(fluids.getFirst().getStored().resource().is(Fluids.WATER), Component.literal("Expected water, got " + fluids.getFirst().getStored()));
        helper.assertValueEqual(fluids.getFirst().getStored().amount(), 1_000L, "fluid amount synced by The One Probe");
        helper.succeed();
    }

    //WTHIT

    /**
     * Runs the block data providers WTHIT registered for the block entity (like its server does on a block data request), and sends Mekanism's
     * data through its registered network codec.
     */
    private static List<ILookingAtElement> wthitElements(GameTestHelper helper, BlockPos pos) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        List<IDataProvider<BlockEntity>> providers = Registrar.get().blockData.get(blockEntity).stream().map(IInstanceRegistry.Entry::instance).toList();
        helper.assertTrue(providers.stream().anyMatch(WTHITDataProvider.class::isInstance),
              Component.literal("Expected Mekanism's WTHIT block data provider to be registered, got " + providers));
        List<IData> data = new ArrayList<>();
        IDataWriter writer = new IDataWriter() {
            private final CompoundTag raw = new CompoundTag();

            @Override
            public CompoundTag raw() {
                return raw;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <D extends IData> void add(IData.Type<D> type, Consumer<Result<D>> consumer) {
                consumer.accept(new Result<>() {
                    @Override
                    public Result<D> add(D d) {
                        data.add(d);
                        return this;
                    }

                    @Override
                    public Result<D> block() {
                        return this;
                    }
                });
            }

            @Override
            public void addImmediate(IData d) {
                data.add(d);
            }
        };
        ServerPlayer player = player(helper, pos);
        for (IDataProvider<BlockEntity> provider : providers) {
            if (provider instanceof WTHITDataProvider) {
                provider.appendData(writer, ServerAccessor.INSTANCE.set(helper.getLevel(), player, hit(helper, pos), blockEntity), PluginConfig.SERVER);
            }
        }
        List<WTHITLookingAtHelper> mekData = data.stream().filter(WTHITLookingAtHelper.class::isInstance).map(WTHITLookingAtHelper.class::cast).toList();
        helper.assertValueEqual(mekData.size(), 1, "Mekanism WTHIT data entries");
        helper.assertTrue(Registrar.get().dataCodecs.containsKey(WTHITLookingAtHelper.TYPE.id()),
              Component.literal("Expected Mekanism's WTHIT data type to be registered"));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        Registrar.get().dataCodecs.get(WTHITLookingAtHelper.TYPE.id()).encode(buffer, mekData.getFirst());
        return ((WTHITLookingAtHelper) Registrar.get().dataCodecs.get(WTHITLookingAtHelper.TYPE.id()).decode(buffer)).elements;
    }

    @GameTest
    public void wthitSyncsEnergyCubeEnergy(GameTestHelper helper) {
        EnergyCubeSetup cube = energyCube(helper);
        EnergyElement energy = single(helper, wthitElements(helper, POS), EnergyElement.class);
        helper.assertValueEqual(energy.getEnergy(), 12_345L, "energy synced by WTHIT");
        helper.assertValueEqual(energy.getMaxEnergy(), cube.capacity(), "max energy synced by WTHIT");
        helper.succeed();
    }

    @GameTest
    public void wthitSyncsChemicalTankChemical(GameTestHelper helper) {
        ChemicalResource hydrogen = chemicalTank(helper);
        ChemicalElement chemical = single(helper, wthitElements(helper, POS), ChemicalElement.class);
        helper.assertValueEqual(chemical.getStored().resource(), hydrogen, "chemical synced by WTHIT");
        helper.assertValueEqual(chemical.getStored().amount(), 1_000L, "chemical amount synced by WTHIT");
        helper.succeed();
    }
}
