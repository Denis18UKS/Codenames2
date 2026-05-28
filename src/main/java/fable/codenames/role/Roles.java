package fable.codenames.role;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentStateManager;

public final class Roles {
    private Roles() {
    }

    public static RoleState getState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getOverworld().getPersistentStateManager();
        return persistentStateManager.getOrCreate(RoleState::createFromNbt, RoleState::new, RoleState.KEY);
    }
}
