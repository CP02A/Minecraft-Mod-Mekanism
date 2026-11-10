package mekanism.additions.common.block.plastic;

import mekanism.api.text.EnumColor;
import mekanism.common.block.interfaces.IColoredBlock;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.IStateFluidLoggable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;

/**
 * Fabric version: vanilla's fence gates only have a constructor taking a wood type (NeoForge adds one taking the sounds directly), so use an unregistered
 * wood type with the same sounds. Its block sound type is the default one (stone), like plastic's other blocks.
 */
public class BlockPlasticFenceGate extends FenceGateBlock implements IColoredBlock, IStateFluidLoggable {

    private static final WoodType PLASTIC = new WoodType("mekanismadditions_plastic", BlockSetType.STONE, SoundType.STONE, SoundType.HANGING_SIGN,
          SoundEvents.FENCE_GATE_CLOSE, SoundEvents.FENCE_GATE_OPEN);

    private final EnumColor color;

    public BlockPlasticFenceGate(BlockBehaviour.Properties properties, EnumColor color) {
        super(PLASTIC, BlockStateHelper.applyLightLevelAdjustments(properties.mapColor(color.getMapColor()).strength(5, 6)));
        this.color = color;
        //Uses getDefaultState as starting state to take into account the stuff from super
        registerDefaultState(BlockStateHelper.getDefaultState(defaultBlockState()));
    }

    @Override
    public EnumColor getColor() {
        return color;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return BlockStateHelper.getStateForPlacementNN(super.getStateForPlacement(context), context);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return getFluid(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos currentPos,
          Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {
        updateFluids(level, currentPos, state, scheduledTickAccess);
        return super.updateShape(state, level, scheduledTickAccess, currentPos, facing, facingPos, facingState, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        BlockStateHelper.fillBlockStateContainer(this, builder);
    }
}
