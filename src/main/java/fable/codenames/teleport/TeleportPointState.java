package fable.codenames.teleport;

import fable.codenames.role.PlayerRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.Map;

public class TeleportPointState extends PersistentState {
    public static final String KEY = "codenames_teleport_points";

    private final Map<String, BlockPos> points = new HashMap<>();

    public static TeleportPointState get(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(TeleportPointState::createFromNbt, TeleportPointState::new, KEY);
    }

    public static TeleportPointState createFromNbt(NbtCompound nbt) {
        TeleportPointState state = new TeleportPointState();
        NbtCompound pointsNbt = nbt.getCompound("points");
        for (String key : pointsNbt.getKeys()) {
            NbtCompound posNbt = pointsNbt.getCompound(key);
            state.points.put(key, new BlockPos(posNbt.getInt("x"), posNbt.getInt("y"), posNbt.getInt("z")));
        }
        return state;
    }

    public BlockPos get(String teamName, PlayerRole role) {
        return this.points.get(key(teamName, role));
    }

    public BlockPos get(String key) {
        return this.points.get(key);
    }

    public void set(String teamName, PlayerRole role, BlockPos pos) {
        this.points.put(key(teamName, role), pos.toImmutable());
        markDirty();
    }

    public void set(String key, BlockPos pos) {
        this.points.put(key, pos.toImmutable());
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound pointsNbt = new NbtCompound();
        for (Map.Entry<String, BlockPos> entry : this.points.entrySet()) {
            NbtCompound posNbt = new NbtCompound();
            posNbt.putInt("x", entry.getValue().getX());
            posNbt.putInt("y", entry.getValue().getY());
            posNbt.putInt("z", entry.getValue().getZ());
            pointsNbt.put(entry.getKey(), posNbt);
        }
        nbt.put("points", pointsNbt);
        return nbt;
    }

    private static String key(String teamName, PlayerRole role) {
        return teamName + "|" + role.getId();
    }
}
