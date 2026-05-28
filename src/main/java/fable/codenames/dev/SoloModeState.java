package fable.codenames.dev;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

import java.util.UUID;

public class SoloModeState extends PersistentState {
    public static final String KEY = "codenames_solo_mode";
    private UUID activePlayer;

    public static SoloModeState createFromNbt(NbtCompound nbt) {
        SoloModeState state = new SoloModeState();
        if (nbt.containsUuid("activePlayer")) {
            state.activePlayer = nbt.getUuid("activePlayer");
        }
        return state;
    }

    public UUID getActivePlayer() {
        return this.activePlayer;
    }

    public void setActivePlayer(UUID activePlayer) {
        this.activePlayer = activePlayer;
        markDirty();
    }

    public void clear() {
        this.activePlayer = null;
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        if (this.activePlayer != null) {
            nbt.putUuid("activePlayer", this.activePlayer);
        }
        return nbt;
    }
}
