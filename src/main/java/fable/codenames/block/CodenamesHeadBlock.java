package fable.codenames.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public class CodenamesHeadBlock extends HorizontalFacingBlock {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final BooleanProperty WALL = BooleanProperty.of("wall");
    private static final VoxelShape FLOOR_SHAPE = Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);
    private static final VoxelShape NORTH_WALL_SHAPE = Block.createCuboidShape(4.0, 4.0, 8.0, 12.0, 12.0, 16.0);
    private static final VoxelShape SOUTH_WALL_SHAPE = Block.createCuboidShape(4.0, 4.0, 0.0, 12.0, 12.0, 8.0);
    private static final VoxelShape WEST_WALL_SHAPE = Block.createCuboidShape(8.0, 4.0, 4.0, 16.0, 12.0, 12.0);
    private static final VoxelShape EAST_WALL_SHAPE = Block.createCuboidShape(0.0, 4.0, 4.0, 8.0, 12.0, 12.0);

    public CodenamesHeadBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(WALL, false));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        Direction side = context.getSide();
        if (side.getAxis().isHorizontal()) {
            return getDefaultState().with(FACING, side).with(WALL, true);
        }

        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite()).with(WALL, false);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, WALL);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getShape(state);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    private static VoxelShape getShape(BlockState state) {
        if (!state.get(WALL)) {
            return FLOOR_SHAPE;
        }

        return switch (state.get(FACING)) {
            case NORTH -> NORTH_WALL_SHAPE;
            case SOUTH -> SOUTH_WALL_SHAPE;
            case WEST -> WEST_WALL_SHAPE;
            case EAST -> EAST_WALL_SHAPE;
            default -> FLOOR_SHAPE;
        };
    }
}
