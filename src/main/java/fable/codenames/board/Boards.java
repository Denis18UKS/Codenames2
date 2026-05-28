package fable.codenames.board;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentStateManager;

public final class Boards {
    private Boards() {
    }

    public static BoardState getState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getOverworld().getPersistentStateManager();
        return persistentStateManager.getOrCreate(BoardState::createFromNbt, BoardState::new, BoardState.KEY);
    }
}
