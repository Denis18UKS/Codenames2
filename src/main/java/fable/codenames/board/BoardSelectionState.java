package fable.codenames.board;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BoardSelectionState {
    private static final Map<UUID, Vote> VOTES_BY_PLAYER = new HashMap<>();
    private static final Map<String, Confirmation> CONFIRMATIONS_BY_TEAM = new HashMap<>();

    private BoardSelectionState() {
    }

    public static Vote getVote(UUID playerId) {
        return VOTES_BY_PLAYER.get(playerId);
    }

    public static void setVote(UUID playerId, String teamName, BlockPos pos, BlockPos displayPos) {
        VOTES_BY_PLAYER.put(playerId, new Vote(teamName, pos.toImmutable(), displayPos.toImmutable()));
    }

    public static void clearVote(UUID playerId) {
        VOTES_BY_PLAYER.remove(playerId);
    }

    public static void clearVotesForTeam(String teamName) {
        VOTES_BY_PLAYER.values().removeIf(vote -> vote.teamName().equals(teamName));
        CONFIRMATIONS_BY_TEAM.remove(teamName);
    }


    public static void clearVotesForPos(BlockPos pos) {
        VOTES_BY_PLAYER.values().removeIf(vote -> vote.pos().equals(pos));
        CONFIRMATIONS_BY_TEAM.entrySet().removeIf(entry -> entry.getValue().pos().equals(pos));
    }

    public static void clearAll() {
        VOTES_BY_PLAYER.clear();
        CONFIRMATIONS_BY_TEAM.clear();
    }

    public static List<VoteIndicator> getIndicators() {
        Map<String, Integer> counts = new HashMap<>();
        Map<String, BlockPos> displayPositions = new HashMap<>();
        for (Vote vote : VOTES_BY_PLAYER.values()) {
            String key = key(vote.teamName(), vote.pos());
            counts.put(key, counts.getOrDefault(key, 0) + 1);
            displayPositions.putIfAbsent(key, vote.displayPos());
        }

        List<VoteIndicator> indicators = new ArrayList<>();
        counts.forEach((key, count) -> {
            String[] parts = key.split(";", 5);
            String teamName = parts[0];
            BlockPos pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            Confirmation confirmation = CONFIRMATIONS_BY_TEAM.get(teamName);
            long confirmationStartTick = confirmation != null && confirmation.pos().equals(pos) ? confirmation.startTick() : -1L;
            indicators.add(new VoteIndicator(teamName, pos, displayPositions.getOrDefault(key, pos), count, confirmationStartTick));
        });
        return List.copyOf(indicators);
    }

    public static void startConfirmation(String teamName, BlockPos pos, long tick) {
        CONFIRMATIONS_BY_TEAM.put(teamName, new Confirmation(pos.toImmutable(), tick));
    }

    public static void clearConfirmation(String teamName) {
        CONFIRMATIONS_BY_TEAM.remove(teamName);
    }

    public static Confirmation getConfirmation(String teamName) {
        return CONFIRMATIONS_BY_TEAM.get(teamName);
    }

    public static List<String> getConfirmingTeams() {
        return List.copyOf(CONFIRMATIONS_BY_TEAM.keySet());
    }

    private static String key(String teamName, BlockPos pos) {
        return teamName + ";" + pos.getX() + ";" + pos.getY() + ";" + pos.getZ() + ";";
    }

    public record Vote(String teamName, BlockPos pos, BlockPos displayPos) {
    }

    public record VoteIndicator(String teamName, BlockPos pos, BlockPos displayPos, int count, long confirmationStartTick) {
    }

    public record Confirmation(BlockPos pos, long startTick) {
    }
}
