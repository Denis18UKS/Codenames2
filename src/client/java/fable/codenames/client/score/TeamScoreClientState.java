package fable.codenames.client.score;

import java.util.HashMap;
import java.util.Map;

public final class TeamScoreClientState {
    private static final Map<String, Integer> SCORES = new HashMap<>();

    private TeamScoreClientState() {
    }

    public static void setScores(Map<String, Integer> scores) {
        SCORES.clear();
        SCORES.putAll(scores);
    }

    public static int getScore(String teamName) {
        return SCORES.getOrDefault(teamName, 0);
    }
}
