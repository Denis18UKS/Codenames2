package fable.codenames.head;

import fable.codenames.block.CodenamesHeadBlock;
import fable.codenames.board.BoardService;
import fable.codenames.board.BoardState;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class HeadRandomizeManager {
    private HeadRandomizeManager() {
    }

    public static int randomize(MinecraftServer server, BoardState board) {
        List<BlockPos> boardPositions = board.getOrderedPositions();
        if (boardPositions.size() != BoardService.TOTAL_CELLS) {
            throw new IllegalStateException("Board must contain exactly 21 cells before head randomizing.");
        }

        ServerWorld world = server.getOverworld();
        List<TargetHeadField> targetFields = detectRegisteredHeadFields(world, board);
        List<HeadDeck.Entry> pickedHeads = pickHeads(server, BoardService.TOTAL_CELLS);
        List<String> pickedIds = new ArrayList<>(pickedHeads.size());

        for (int field = 0; field < targetFields.size(); field++) {
            TargetHeadField targetField = targetFields.get(field);
            for (int i = 0; i < targetField.positions().size(); i++) {
                BlockPos headPos = targetField.positions().get(i);
                HeadDeck.Entry pick = pickedHeads.get(i);
                world.setBlockState(headPos, copyPlacement(pick.block().getDefaultState(), targetField.side()));
                if (field == 0) {
                    pickedIds.add(pick.id());
                }
            }
        }

        HeadDeckState.get(server).markUsed(pickedIds);
        return pickedHeads.size() * targetFields.size();
    }

    public static void resetWeights(MinecraftServer server) {
        HeadDeckState.get(server).resetWeights();
    }

    private static List<HeadDeck.Entry> pickHeads(MinecraftServer server, int count) {
        HeadDeckState state = HeadDeckState.get(server);
        List<HeadDeck.Entry> pool = new ArrayList<>(HeadDeck.entries(state));
        if (pool.size() < count) {
            throw new IllegalStateException("Not enough registered heads for the board.");
        }

        Random random = new Random(server.getOverworld().getSeed() ^ server.getTicks());
        List<HeadDeck.Entry> picks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int index = weightedIndex(pool, random);
            picks.add(pool.remove(index));
        }
        return picks;
    }

    private static int weightedIndex(List<HeadDeck.Entry> pool, Random random) {
        int totalWeight = 0;
        for (HeadDeck.Entry entry : pool) {
            totalWeight += entry.weight();
        }

        int roll = random.nextInt(totalWeight);
        for (int i = 0; i < pool.size(); i++) {
            roll -= pool.get(i).weight();
            if (roll < 0) {
                return i;
            }
        }
        return pool.size() - 1;
    }

    private static List<TargetHeadField> detectRegisteredHeadFields(ServerWorld world, BoardState board) {
        List<TargetHeadField> headFields = new ArrayList<>();
        BlockPos firstRegisteredBoard = null;
        for (List<BlockPos> field : board.getFields()) {
            if (field.size() != BoardService.TOTAL_CELLS) {
                continue;
            }
            if (firstRegisteredBoard == null && !field.isEmpty()) {
                firstRegisteredBoard = field.get(0);
            }

            Direction headSide = detectHeadSide(world, field);
            if (headSide == null) {
                continue;
            }
            List<BlockPos> headPositions = new ArrayList<>(field.size());
            for (BlockPos boardPos : field) {
                headPositions.add(boardPos.offset(headSide).toImmutable());
            }
            headFields.add(new TargetHeadField(List.copyOf(headPositions), headSide));
        }

        if (headFields.isEmpty()) {
            if (firstRegisteredBoard != null) {
                throw new IllegalStateException("No codenames heads found next to registered board at " + firstRegisteredBoard.toShortString() + ".");
            }
            throw new IllegalStateException("No complete codenames head fields found next to registered boards.");
        }
        return List.copyOf(headFields);
    }

    private static Direction detectHeadSide(ServerWorld world, List<BlockPos> field) {
        Direction[] candidates = sideCandidates(field);
        int first = countHeads(world, field, candidates[0]);
        int second = countHeads(world, field, candidates[1]);
        if (first == 0 && second == 0) {
            return null;
        }
        return first >= second ? candidates[0] : candidates[1];
    }

    private static Direction[] sideCandidates(List<BlockPos> field) {
        BlockPos first = field.get(0);
        boolean sameX = field.stream().allMatch(pos -> pos.getX() == first.getX());
        boolean sameZ = field.stream().allMatch(pos -> pos.getZ() == first.getZ());
        if (sameX == sameZ) {
            throw new IllegalStateException("Registered board must be a vertical 7x3 plane.");
        }
        return sameX ? new Direction[]{Direction.EAST, Direction.WEST} : new Direction[]{Direction.SOUTH, Direction.NORTH};
    }

    private static int countHeads(ServerWorld world, List<BlockPos> field, Direction side) {
        int count = 0;
        for (BlockPos boardPos : field) {
            if (isHeadMarker(world.getBlockState(boardPos.offset(side)))) {
                count++;
            }
        }
        return count;
    }

    private static boolean isHeadMarker(BlockState state) {
        return state.getBlock() instanceof CodenamesHeadBlock || state.getBlock() instanceof AbstractSkullBlock;
    }

    private static BlockState copyPlacement(BlockState replacement, Direction headSide) {
        return replacement
                .with(CodenamesHeadBlock.WALL, true)
                .with(CodenamesHeadBlock.FACING, headSide);
    }

    private record TargetHeadField(List<BlockPos> positions, Direction side) {
    }
}
