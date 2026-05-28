package fable.codenames.board;

import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BoardSelections {
    private static final Map<UUID, BlockPos> FIRST_CORNERS = new ConcurrentHashMap<>();

    private BoardSelections() {
    }

    public static BlockPos getFirstCorner(UUID playerId) {
        return FIRST_CORNERS.get(playerId);
    }

    public static void setFirstCorner(UUID playerId, BlockPos pos) {
        FIRST_CORNERS.put(playerId, pos.toImmutable());
    }

    public static BlockPos clearFirstCorner(UUID playerId) {
        return FIRST_CORNERS.remove(playerId);
    }
}
