package fable.codenames.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentStateManager;

public final class CodenamesGames {
    private CodenamesGames() {
    }

    public static CodenamesGameState getState(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(CodenamesGameState::createFromNbt, CodenamesGameState::new, CodenamesGameState.KEY);
    }
}
