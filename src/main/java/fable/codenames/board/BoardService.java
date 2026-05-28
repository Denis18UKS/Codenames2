package fable.codenames.board;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class BoardService {
    public static final int BOARD_WIDTH = 7;
    public static final int BOARD_HEIGHT = 3;
    public static final int TOTAL_CELLS = 21;
    public static final int RED_COUNT = 8;
    public static final int BLUE_COUNT = 8;
    public static final int NEUTRAL_COUNT = 4;
    public static final int ASSASSIN_COUNT = 1;

    private BoardService() {
    }

    public static List<BlockPos> createBoardPositions(BlockPos first, BlockPos second) {
        boolean sameX = first.getX() == second.getX();
        boolean sameZ = first.getZ() == second.getZ();

        if (sameX == sameZ) {
            throw new IllegalArgumentException("Поле должно стоять на вертикальной стене.");
        }

        int minY = Math.min(first.getY(), second.getY());
        int maxY = Math.max(first.getY(), second.getY());
        int height = maxY - minY + 1;
        if (height != BOARD_HEIGHT) {
            throw new IllegalArgumentException("Высота поля должна быть ровно 3 блока.");
        }

        List<BlockPos> positions = new ArrayList<>(TOTAL_CELLS);
        if (sameX) {
            int minZ = Math.min(first.getZ(), second.getZ());
            int maxZ = Math.max(first.getZ(), second.getZ());
            int width = maxZ - minZ + 1;
            if (width != BOARD_WIDTH) {
                throw new IllegalArgumentException("Ширина поля должна быть ровно 7 блоков.");
            }

            for (int y = maxY; y >= minY; y--) {
                for (int z = minZ; z <= maxZ; z++) {
                    positions.add(new BlockPos(first.getX(), y, z));
                }
            }
            return positions;
        }

        int minX = Math.min(first.getX(), second.getX());
        int maxX = Math.max(first.getX(), second.getX());
        int width = maxX - minX + 1;
        if (width != BOARD_WIDTH) {
            throw new IllegalArgumentException("Ширина поля должна быть ровно 7 блоков.");
        }

        for (int y = maxY; y >= minY; y--) {
            for (int x = minX; x <= maxX; x++) {
                positions.add(new BlockPos(x, y, first.getZ()));
            }
        }
        return positions;
    }

    public static Map<BoardCellType, Integer> countByType(BoardState state) {
        EnumMap<BoardCellType, Integer> counts = new EnumMap<>(BoardCellType.class);
        for (BoardCellType type : BoardCellType.values()) {
            counts.put(type, 0);
        }
        for (BoardCellType type : state.getCells().values()) {
            counts.computeIfPresent(type, (key, value) -> value + 1);
        }
        return counts;
    }

    public static void randomize(BoardState state, MinecraftServer server) {
        List<BlockPos> positions = new ArrayList<>(state.getOrderedPositions());
        if (positions.size() != TOTAL_CELLS) {
            throw new IllegalStateException("Для рандомизации нужно ровно 21 объект.");
        }

        Collections.shuffle(positions, new Random(server.getOverworld().getSeed() ^ server.getTicks()));

        for (int i = 0; i < positions.size(); i++) {
            BoardCellType type;
            if (i < RED_COUNT) {
                type = BoardCellType.RED;
            } else if (i < RED_COUNT + BLUE_COUNT) {
                type = BoardCellType.BLUE;
            } else if (i < RED_COUNT + BLUE_COUNT + NEUTRAL_COUNT) {
                type = BoardCellType.NEUTRAL;
            } else {
                type = BoardCellType.ASSASSIN;
            }
            state.setType(positions.get(i), type);
        }
    }

    public static MutableText progressText(BoardState state) {
        Map<BoardCellType, Integer> counts = countByType(state);
        int total = state.size();
        boolean valid = total == TOTAL_CELLS
                && counts.get(BoardCellType.RED) == RED_COUNT
                && counts.get(BoardCellType.BLUE) == BLUE_COUNT
                && counts.get(BoardCellType.NEUTRAL) == NEUTRAL_COUNT
                && counts.get(BoardCellType.ASSASSIN) == ASSASSIN_COUNT;

        MutableText text = Text.literal("Поле: " + total + "/" + TOTAL_CELLS).formatted(Formatting.GOLD)
                .append(Text.literal(" | Красные " + counts.get(BoardCellType.RED) + "/" + RED_COUNT).formatted(Formatting.RED))
                .append(Text.literal(" | Синие " + counts.get(BoardCellType.BLUE) + "/" + BLUE_COUNT).formatted(Formatting.AQUA))
                .append(Text.literal(" | Нейтральные " + counts.get(BoardCellType.NEUTRAL) + "/" + NEUTRAL_COUNT).formatted(Formatting.GRAY))
                .append(Text.literal(" | Убийца " + counts.get(BoardCellType.ASSASSIN) + "/" + ASSASSIN_COUNT).formatted(Formatting.DARK_GRAY));

        if (counts.get(BoardCellType.UNASSIGNED) > 0) {
            text.append(Text.literal(" | Не назначено " + counts.get(BoardCellType.UNASSIGNED)).formatted(Formatting.YELLOW));
        }

        text.append(Text.literal(valid ? " | Готово" : " | Ещё не готово")
                .formatted(valid ? Formatting.GREEN : Formatting.YELLOW));
        return text;
    }
}
