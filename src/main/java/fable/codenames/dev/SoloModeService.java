package fable.codenames.dev;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentStateManager;

import java.util.UUID;

public final class SoloModeService {
    private SoloModeService() {
    }

    public static SoloModeState getState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getOverworld().getPersistentStateManager();
        return persistentStateManager.getOrCreate(SoloModeState::createFromNbt, SoloModeState::new, SoloModeState.KEY);
    }

    public static boolean isEnabled(MinecraftServer server, UUID playerId) {
        UUID activePlayer = getState(server).getActivePlayer();
        return activePlayer != null && activePlayer.equals(playerId);
    }

    public static void enable(MinecraftServer server, UUID playerId) {
        getState(server).setActivePlayer(playerId);
    }

    public static void disable(MinecraftServer server) {
        getState(server).clear();
    }
}
