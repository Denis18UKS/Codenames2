package fable.codenames.game;

import fable.codenames.board.BoardCellType;
import fable.codenames.board.BoardState;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;

public final class CodenamesGameSnapshot {
    private static final LinkedHashMap<BlockPos, BoardCellType> BOARD_CELLS = new LinkedHashMap<>();
    private static final LinkedHashMap<BlockPos, BlockState> BLOCK_STATES = new LinkedHashMap<>();

    private CodenamesGameSnapshot() {
    }

    public static void capture(ServerWorld world, BoardState board) {
        BOARD_CELLS.clear();
        BLOCK_STATES.clear();
        board.getCells().forEach((pos, type) -> {
            BlockPos immutablePos = pos.toImmutable();
            BOARD_CELLS.put(immutablePos, type);
            for (BlockPos linkedPos : board.getLinkedPositions(immutablePos)) {
                BLOCK_STATES.put(linkedPos.toImmutable(), world.getBlockState(linkedPos));
            }
        });
    }

    public static boolean restore(ServerWorld world, BoardState board) {
        if (BOARD_CELLS.isEmpty()) {
            return false;
        }

        BLOCK_STATES.forEach(world::setBlockState);
        board.setCells(BOARD_CELLS);
        return true;
    }

    public static void clear() {
        BOARD_CELLS.clear();
        BLOCK_STATES.clear();
    }
}
