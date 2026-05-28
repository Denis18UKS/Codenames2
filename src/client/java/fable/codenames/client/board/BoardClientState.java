package fable.codenames.client.board;

import fable.codenames.board.BoardCellType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BoardClientState {
    private static final int FIELD_SIZE = 21;

    private static final LinkedHashMap<BlockPos, BoardCellType> CELLS = new LinkedHashMap<>();
    private static List<VoteIndicator> VOTE_INDICATORS = List.of();
    private static boolean canSeeAnswers;

    private BoardClientState() {
    }

    public static void setCells(Map<BlockPos, BoardCellType> cells) {
        CELLS.clear();
        CELLS.putAll(cells);
    }

    public static Map<BlockPos, BoardCellType> getCells() {
        return Map.copyOf(CELLS);
    }

    public static BlockPos displayPosFor(BlockPos pos, Vec3d cameraPos) {
        BlockPos best = pos;
        double bestDistance = Double.MAX_VALUE;
        int canonicalIndex = canonicalIndexFor(pos);
        if (canonicalIndex < 0) {
            return pos;
        }

        for (BlockPos cellPos : CELLS.keySet()) {
            int index = indexOf(cellPos);
            if (index >= 0 && index % FIELD_SIZE == canonicalIndex) {
                double distance = cellPos.getSquaredDistance(cameraPos);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = cellPos;
                }
            }
        }
        return best;
    }

    public static List<BlockPos> fieldFor(BlockPos pos) {
        List<BlockPos> positions = new ArrayList<>(CELLS.keySet());
        int index = positions.indexOf(pos);
        if (index < 0) {
            return positions;
        }

        int start = (index / FIELD_SIZE) * FIELD_SIZE;
        int end = Math.min(start + FIELD_SIZE, positions.size());
        return List.copyOf(positions.subList(start, end));
    }

    public static void setCanSeeAnswers(boolean value) {
        canSeeAnswers = value;
    }

    public static boolean canSeeAnswers() {
        return canSeeAnswers;
    }

    public static void clear() {
        CELLS.clear();
        VOTE_INDICATORS = List.of();
        canSeeAnswers = false;
    }

    public static void setVoteIndicators(List<VoteIndicator> voteIndicators) {
        VOTE_INDICATORS = List.copyOf(voteIndicators);
    }

    public static List<VoteIndicator> getVoteIndicators() {
        return VOTE_INDICATORS;
    }

    public record VoteIndicator(String teamName, BlockPos pos, BlockPos displayPos, int count, long confirmationStartTick) {
    }

    private static int canonicalIndexFor(BlockPos pos) {
        int index = 0;
        for (BlockPos cellPos : CELLS.keySet()) {
            if (cellPos.equals(pos)) {
                return index % FIELD_SIZE;
            }
            index++;
        }
        return -1;
    }

    private static int indexOf(BlockPos pos) {
        int index = 0;
        for (BlockPos cellPos : CELLS.keySet()) {
            if (cellPos.equals(pos)) {
                return index;
            }
            index++;
        }
        return -1;
    }
}
