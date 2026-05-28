package fable.codenames.block;

import fable.codenames.block.entity.ClickButtonBlockEntity;
import fable.codenames.game.CodenamesGameService;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ClickButtonBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final EnumProperty<PlacementOffset> PLACEMENT_OFFSET = EnumProperty.of("placement_offset", PlacementOffset.class);
    private static final VoxelShape SHAPE = Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 4.0, 12.0);
    private static final VoxelShape START_BUTTON_SHAPE = Block.createCuboidShape(4.35, 0.0, 4.35, 11.65, 15.6, 11.65);
    private static final int PRESS_TICKS = 20;
    private final ClickButtonBlockEntity.Mode defaultMode;

    public ClickButtonBlock(Settings settings) {
        this(settings, ClickButtonBlockEntity.Mode.NORMAL);
    }

    public ClickButtonBlock(Settings settings, ClickButtonBlockEntity.Mode defaultMode) {
        super(settings);
        this.defaultMode = defaultMode;
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWERED, false)
                .with(PLACEMENT_OFFSET, PlacementOffset.CENTER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING, POWERED, PLACEMENT_OFFSET);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        PlacementOffset offset = this.defaultMode == ClickButtonBlockEntity.Mode.RESET
                ? getStartButtonPlacementOffset(ctx)
                : PlacementOffset.CENTER;
        return getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(POWERED, false)
                .with(PLACEMENT_OFFSET, offset);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (!state.isOf(ModBlocks.START_BUTTON.getBlock())) {
            return SHAPE;
        }
        PlacementOffset offset = state.get(PLACEMENT_OFFSET);
        return START_BUTTON_SHAPE.offset(offset.getX() * 0.5, 0.0, offset.getZ() * 0.5);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        ClickButtonBlockEntity entity = new ClickButtonBlockEntity(pos, state);
        entity.setMode(this.defaultMode);
        return entity;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (world.getBlockEntity(pos) instanceof ClickButtonBlockEntity entity) {
            entity.triggerClick();
            if (entity.getMode() == ClickButtonBlockEntity.Mode.RESET) {
                world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.8F, 1.0F);
                if (player.getServer() != null) {
                    CodenamesGameService.resetWithoutLobbyTeleport(player.getServer());
                }
                return ActionResult.CONSUME;
            }
            if (entity.getMode() == ClickButtonBlockEntity.Mode.PASS_TURN) {
                world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.8F, 1.0F);
                if (player.getServer() != null && player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                    CodenamesGameService.tryPassTurn(player.getServer(), serverPlayer);
                }
                return ActionResult.CONSUME;
            }
        }

        press(state, world, pos);
        return ActionResult.CONSUME;
    }

    private void press(BlockState state, World world, BlockPos pos) {
        if (state.get(POWERED)) {
            return;
        }

        BlockState poweredState = state.with(POWERED, true);
        world.setBlockState(pos, poweredState, Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 0.8F, 1.0F);
        world.scheduleBlockTick(pos, this, PRESS_TICKS);
        updatePowerNeighbors(poweredState, world, pos);
    }

    @Override
    public void scheduledTick(BlockState state, net.minecraft.server.world.ServerWorld world, BlockPos pos, Random random) {
        if (!state.get(POWERED)) {
            return;
        }

        BlockState unpoweredState = state.with(POWERED, false);
        world.setBlockState(pos, unpoweredState, Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 0.8F, 1.0F);
        updatePowerNeighbors(unpoweredState, world, pos);
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) && direction == Direction.DOWN ? 15 : 0;
    }

    private void updatePowerNeighbors(BlockState state, World world, BlockPos pos) {
        BlockPos supportPos = pos.down();
        BlockPos commandBlockPos = supportPos.down();

        world.updateNeighbors(pos, this);
        world.updateNeighbors(supportPos, this);
        world.updateNeighbors(commandBlockPos, this);

        for (Direction direction : Direction.values()) {
            world.updateNeighbors(pos.offset(direction), this);
            world.updateNeighbors(supportPos.offset(direction), this);
            world.updateNeighbors(commandBlockPos.offset(direction), this);
        }
        world.updateComparators(pos, this);
        world.updateComparators(supportPos, this);
        world.updateComparators(commandBlockPos, this);
    }

    private static PlacementOffset getStartButtonPlacementOffset(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        Vec3d hit = ctx.getHitPos();
        double localX = hit.x - pos.getX();
        double localZ = hit.z - pos.getZ();

        double west = localX;
        double east = 1.0 - localX;
        double north = localZ;
        double south = 1.0 - localZ;
        double nearest = Math.min(Math.min(west, east), Math.min(north, south));

        if (nearest > 0.25) {
            return PlacementOffset.CENTER;
        }
        if (nearest == west) {
            return PlacementOffset.WEST;
        }
        if (nearest == east) {
            return PlacementOffset.EAST;
        }
        if (nearest == north) {
            return PlacementOffset.NORTH;
        }
        return PlacementOffset.SOUTH;
    }

    public enum PlacementOffset implements StringIdentifiable {
        CENTER("center", 0, 0),
        NORTH("north", 0, -1),
        SOUTH("south", 0, 1),
        WEST("west", -1, 0),
        EAST("east", 1, 0);

        private final String name;
        private final int x;
        private final int z;

        PlacementOffset(String name, int x, int z) {
            this.name = name;
            this.x = x;
            this.z = z;
        }

        @Override
        public String asString() {
            return this.name;
        }

        public int getX() {
            return this.x;
        }

        public int getZ() {
            return this.z;
        }
    }
}
