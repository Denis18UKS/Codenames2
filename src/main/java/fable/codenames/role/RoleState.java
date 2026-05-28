package fable.codenames.role;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoleState extends PersistentState {
    public static final String KEY = "codenames_roles";
    private final Map<UUID, String> roles = new HashMap<>();

    public static RoleState createFromNbt(NbtCompound nbt) {
        RoleState state = new RoleState();
        NbtCompound rolesNbt = nbt.getCompound("roles");

        for (String key : rolesNbt.getKeys()) {
            try {
                state.roles.put(UUID.fromString(key), rolesNbt.getString(key));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return state;
    }

    public PlayerRole getRole(UUID uuid) {
        return PlayerRole.fromId(this.roles.get(uuid));
    }

    public void setRole(UUID uuid, PlayerRole role) {
        this.roles.put(uuid, role.getId());
        markDirty();
    }

    public void clearRole(UUID uuid) {
        this.roles.remove(uuid);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound rolesNbt = new NbtCompound();
        this.roles.forEach((uuid, role) -> rolesNbt.putString(uuid.toString(), role));
        nbt.put("roles", rolesNbt);
        return nbt;
    }
}
