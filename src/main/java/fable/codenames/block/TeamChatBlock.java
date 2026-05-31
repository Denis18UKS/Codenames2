package fable.codenames.block;

import fable.codenames.chat.TeamChatSync;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class TeamChatBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(-8.0, 0.0, 14.0, 24.0, 64.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(-8.0, 0.0, 0.0, 24.0, 64.0, 2.0);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0.0, 0.0, -8.0, 2.0, 64.0, 24.0);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(14.0, 0.0, -8.0, 16.0, 64.0, 24.0);

    public TeamChatBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    public static BlockPos resolveTeamChat(World world, BlockHitResult hit) {
        BlockPos hitPos = hit.getBlockPos();
        if (world.getBlockState(hitPos).getBlock() instanceof TeamChatBlock) {return hitPos;}
        BlockPos spacePos = hitPos.offset(hit.getSide());
        if (world.getBlockState(spacePos).getBlock() instanceof TeamChatBlock) {return spacePos;}
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos nbPos = spacePos.offset(dir);
            if (world.getBlockState(nbPos).getBlock() instanceof TeamChatBlock) {
                Direction facing = world.getBlockState(nbPos).get(FACING);
                if (dir == facing.rotateYClockwise() || dir == facing.rotateYCounterclockwise()) {return nbPos;}
            }
        }
        return null;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction direction = ctx.getSide();
        if (!direction.getAxis().isHorizontal()) {
            direction = ctx.getHorizontalPlayerFacing().getOpposite();
        }

        BlockState state = getDefaultState().with(FACING, direction);
        return state.canPlaceAt(ctx.getWorld(), ctx.getBlockPos()) ? state : null;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction supportDirection = state.get(FACING).getOpposite();
        BlockPos supportPos = pos.offset(supportDirection);
        return world.getBlockState(supportPos).isSideSolidFullSquare(world, supportPos, state.get(FACING));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        Direction supportDirection = state.get(FACING).getOpposite();
        if (direction == supportDirection && !state.canPlaceAt(world, pos)) {
            return net.minecraft.block.Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new fable.codenames.block.entity.TeamChatBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            TeamChatSync.openForPlayer(serverPlayer, pos);
        }
        return ActionResult.CONSUME;
    }
}