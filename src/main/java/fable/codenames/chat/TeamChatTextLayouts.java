package fable.codenames.chat;

import net.minecraft.server.MinecraftServer;

public final class TeamChatTextLayouts {
    private TeamChatTextLayouts() {
    }

    public static TeamChatTextLayoutState getState(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(TeamChatTextLayoutState::createFromNbt, TeamChatTextLayoutState::new, TeamChatTextLayoutState.KEY);
    }
}
