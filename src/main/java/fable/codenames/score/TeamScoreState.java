package fable.codenames.score;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

public class TeamScoreState extends PersistentState {
    public static final String KEY = "codenames_team_scores";
    private final Map<String, Integer> scores = new HashMap<>();

    public static TeamScoreState createFromNbt(NbtCompound nbt) {
        TeamScoreState state = new TeamScoreState();
        NbtCompound scoresNbt = nbt.getCompound("scores");

        for (String key : scoresNbt.getKeys()) {
            if (scoresNbt.contains(key, NbtElement.INT_TYPE)) {
                state.scores.put(key, scoresNbt.getInt(key));
            }
        }

        return state;
    }

    public Map<String, Integer> getScores() {
        return Map.copyOf(this.scores);
    }

    public int getScore(String teamName) {
        return this.scores.getOrDefault(teamName, 0);
    }

    public void setScore(String teamName, int value) {
        this.scores.put(teamName, value);
        markDirty();
    }

    public int addScore(String teamName, int delta) {
        int updated = getScore(teamName) + delta;
        this.scores.put(teamName, updated);
        markDirty();
        return updated;
    }

    public void resetScore(String teamName) {
        this.scores.remove(teamName);
        markDirty();
    }

    public void clearScores() {
        this.scores.clear();
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound scoresNbt = new NbtCompound();
        this.scores.forEach(scoresNbt::putInt);
        nbt.put("scores", scoresNbt);
        return nbt;
    }
}
