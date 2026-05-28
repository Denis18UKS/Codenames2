package fable.codenames.role;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentStateManager;

public final class Roles {

    private Roles() {}

    public static RoleState getState(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(RoleState::createFromNbt, RoleState::new, RoleState.KEY);
    }

    /**
     * Очистка всех ролей
     */
    public static void clearAll(MinecraftServer server) {
        getState(server).clearAll();
    }
}