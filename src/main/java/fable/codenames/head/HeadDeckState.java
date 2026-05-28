package fable.codenames.head;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class HeadDeckState extends PersistentState {
    public static final String KEY = "codenames_head_deck";

    private final Set<String> usedHeads = new LinkedHashSet<>();

    public static HeadDeckState get(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(HeadDeckState::createFromNbt, HeadDeckState::new, KEY);
    }

    public static HeadDeckState createFromNbt(NbtCompound nbt) {
        HeadDeckState state = new HeadDeckState();
        NbtList list = nbt.getList("used_heads", 8);
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            if (!id.isBlank()) {
                state.usedHeads.add(id);
            }
        }
        return state;
    }

    public boolean isUsed(String id) {
        return this.usedHeads.contains(id);
    }

    public void markUsed(Collection<String> ids) {
        if (this.usedHeads.addAll(ids)) {
            markDirty();
        }
    }

    public void resetWeights() {
        if (!this.usedHeads.isEmpty()) {
            this.usedHeads.clear();
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (String id : this.usedHeads) {
            list.add(NbtString.of(id));
        }
        nbt.put("used_heads", list);
        return nbt;
    }
}
