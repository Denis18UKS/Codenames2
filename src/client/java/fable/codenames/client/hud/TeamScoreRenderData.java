package fable.codenames.client.hud;

import fable.codenames.client.score.TeamScoreClientState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TeamScoreRenderData {

    public static final int PADDING = 4;
    public static final int LINE_HEIGHT = 10;

    private TeamScoreRenderData() {
    }

    public static List<TeamRow> getRows(MinecraftClient client, boolean requirePlayerTeam) {

        if (client.player == null || client.world == null) {
            return List.of();
        }

        if (requirePlayerTeam) {
            AbstractTeam currentTeam = client.player.getScoreboardTeam();

            if (!(currentTeam instanceof Team)) {
                return List.of();
            }
        }

        Scoreboard scoreboard = client.world.getScoreboard();

        return scoreboard.getTeams().stream()
                .map(team -> new TeamRow(
                        getVisibleName(team),
                        TeamScoreClientState.getScore(team.getName()),
                        formattingColor(team)
                ))
                .sorted(Comparator.comparing(TeamRow::sortKey))
                .toList();
    }

    public static Layout measure(TextRenderer textRenderer, List<TeamRow> rows) {

        int maxNameWidth = rows.stream()
                .mapToInt(row -> textRenderer.getWidth(row.label()))
                .max()
                .orElse(0);

        int maxScoreWidth = rows.stream()
                .mapToInt(row -> textRenderer.getWidth(Integer.toString(row.value())))
                .max()
                .orElse(0);

        int width = maxNameWidth + 12 + maxScoreWidth + PADDING * 2;

        int height = rows.size() * LINE_HEIGHT + PADDING * 2;

        return new Layout(width, height);
    }

    private static MutableText getVisibleName(Team team) {

        String normalized = team.getName().toLowerCase(Locale.ROOT);

        if (normalized.contains("red") || normalized.contains("крас")) {
            return Text.literal("Красные").formatted(Formatting.RED);
        }

        if (normalized.contains("blue") || normalized.contains("син")) {
            return Text.literal("Синие").formatted(Formatting.BLUE);
        }

        Text displayName = team.getDisplayName();

        if (displayName == null || displayName.getString().isBlank()) {
            return Text.literal(team.getName())
                    .formatted(formattingColor(team));
        }

        return displayName.copy();
    }

    private static Formatting formattingColor(Team team) {

        Formatting color = team.getColor();

        return color == null || color == Formatting.RESET
                ? Formatting.WHITE
                : color;
    }

    public record TeamRow(
            MutableText label,
            int value,
            Formatting color
    ) {

        public String sortKey() {
            return label.getString().toLowerCase();
        }
    }

    public record Layout(
            int width,
            int height
    ) {
    }
}