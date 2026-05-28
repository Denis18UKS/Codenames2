package fable.codenames.game;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

public class CodenamesRoundState extends PersistentState {
    public static final String KEY = "codenames_rounds";
    public static final int ROOM_COUNT = 3;

    private int nextRoundIndex;

    public static CodenamesRoundState get(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(CodenamesRoundState::createFromNbt, CodenamesRoundState::new, KEY);
    }

    public static CodenamesRoundState createFromNbt(NbtCompound nbt) {
        CodenamesRoundState state = new CodenamesRoundState();
        state.nextRoundIndex = Math.max(0, nbt.getInt("nextRoundIndex"));
        return state;
    }

    public int nextRoundNumber() {
        return this.nextRoundIndex + 1;
    }

    public int nextRoomNumber() {
        return Math.floorMod(this.nextRoundIndex, ROOM_COUNT) + 1;
    }

    public void advanceRound() {
        this.nextRoundIndex++;
        markDirty();
    }

    public void resetRounds() {
        this.nextRoundIndex = 0;
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("nextRoundIndex", this.nextRoundIndex);
        return nbt;
    }
}
