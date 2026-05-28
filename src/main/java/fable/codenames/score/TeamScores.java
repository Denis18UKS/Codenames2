package fable.codenames.score;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentStateManager;

public final class TeamScores {
    private TeamScores() {
    }

    public static TeamScoreState getState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getOverworld().getPersistentStateManager();
        return persistentStateManager.getOrCreate(TeamScoreState::createFromNbt, TeamScoreState::new, TeamScoreState.KEY);
    }
}
