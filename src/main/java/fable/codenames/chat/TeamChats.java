package fable.codenames.chat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentStateManager;

public final class TeamChats {
    private TeamChats() {
    }

    public static TeamChatState getState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getOverworld().getPersistentStateManager();
        return persistentStateManager.getOrCreate(TeamChatState::createFromNbt, TeamChatState::new, TeamChatState.KEY);
    }
}
