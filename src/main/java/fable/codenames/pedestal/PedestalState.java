package fable.codenames.pedestal;

import fable.codenames.role.PlayerRole;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.LinkedHashMap;
import java.util.Map;

public class PedestalState extends PersistentState {
    public static final String KEY = "codenames_pedestals";

    private final LinkedHashMap<BlockPos, Assignment> assignments = new LinkedHashMap<>();

    public static PedestalState get(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(PedestalState::createFromNbt, PedestalState::new, KEY);
    }

    public static PedestalState createFromNbt(NbtCompound nbt) {
        PedestalState state = new PedestalState();
        NbtList list = nbt.getList("pedestals", 10);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            PlayerRole role = PlayerRole.fromId(entry.getString("role"));
            String teamName = entry.getString("team");
            if (role == null || teamName.isBlank()) {
                continue;
            }
            BlockPos pos = new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z"));
            state.assignments.put(pos, new Assignment(teamName, role));
        }
        return state;
    }

    public Map<BlockPos, Assignment> getAssignments() {
        return Map.copyOf(this.assignments);
    }

    public Assignment get(BlockPos pos) {
        return this.assignments.get(pos);
    }

    public void set(BlockPos pos, String teamName, PlayerRole role) {
        this.assignments.put(pos.toImmutable(), new Assignment(teamName, role));
        markDirty();
    }

    public void remove(BlockPos pos) {
        if (this.assignments.remove(pos) != null) {
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Map.Entry<BlockPos, Assignment> entry : this.assignments.entrySet()) {
            NbtCompound pedestal = new NbtCompound();
            pedestal.putInt("x", entry.getKey().getX());
            pedestal.putInt("y", entry.getKey().getY());
            pedestal.putInt("z", entry.getKey().getZ());
            pedestal.putString("team", entry.getValue().teamName());
            pedestal.putString("role", entry.getValue().role().getId());
            list.add(pedestal);
        }
        nbt.put("pedestals", list);
        return nbt;
    }

    public record Assignment(String teamName, PlayerRole role) {
    }
}
