package fable.codenames.client.hud;

import fable.codenames.client.game.GameTimerClientState;
import fable.codenames.game.CodenamesPhase;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.List;

public final class TeamListHud {

    private TeamListHud() {
    }

    public static void init() {
        TeamHudState.load();

        HudRenderCallback.EVENT.register((drawContext, tickDelta) ->
                render(drawContext));
    }

    private static void render(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!TeamHudState.isEnabled()
                || client.player == null
                || client.world == null
                || client.options.hudHidden) {
            return;
        }

        List<TeamScoreRenderData.TeamRow> rows =
                TeamScoreRenderData.getRows(client, true);

        if (rows.isEmpty()) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;

        TeamScoreRenderData.Layout layout =
                TeamScoreRenderData.measure(textRenderer, rows);

        int width = layout.width();
        int height = layout.height();

        int left = TeamHudState.getX();
        int top = TeamHudState.getY();

        drawContext.fill(
                left,
                top,
                left + width,
                top + height,
                0x6A000000
        );

        int y = top + TeamScoreRenderData.PADDING;

        for (TeamScoreRenderData.TeamRow row : rows) {

            drawContext.drawText(
                    textRenderer,
                    row.label(),
                    left + TeamScoreRenderData.PADDING,
                    y,
                    row.color().getColorValue() != null
                            ? row.color().getColorValue()
                            : 0xFFFFFF,
                    false
            );

            String scoreText = Integer.toString(row.value());

            int scoreX = left
                    + width
                    - TeamScoreRenderData.PADDING
                    - textRenderer.getWidth(scoreText);

            drawContext.drawText(
                    textRenderer,
                    scoreText,
                    scoreX,
                    y,
                    0xFFFFFF,
                    false
            );

            y += TeamScoreRenderData.LINE_HEIGHT;
        }

        if (GameTimerClientState.isActive()) {
            String team = GameTimerClientState.getTeamName().toLowerCase();
            CodenamesPhase phase = GameTimerClientState.getPhase();

            boolean red = team.contains("red") || team.contains("крас");
            boolean blue = team.contains("blue") || team.contains("син");

            if (red || blue) {
                boolean leaderPhase = (phase == CodenamesPhase.WAITING_CLUE);
                String textStr;
                int color;

                if (red) {
                    textStr = leaderPhase ? "Ход: лидера Красных" : "Ход: команды Красных";
                    color = 0xFFFF5555;
                } else {
                    textStr = leaderPhase ? "Ход: лидера  Синих" : "Ход: команды Синих";
                    color = 0xFF55AAFF;
                }

                Text turnText = Text.literal(textStr);
                int turnWidth = textRenderer.getWidth(turnText) + 8;
                int turnY = top + height + 3;
                int turnHeight = 14;

                drawContext.fill(
                        left,
                        turnY,
                        left + turnWidth,
                        turnY + turnHeight,
                        0x6A000000
                );

                drawContext.drawText(
                        textRenderer,
                        turnText,
                        left + 4,
                        turnY + 3,
                        color,
                        false
                );
            }
        }
    }

    public static void renderPreview(
            DrawContext drawContext,
            int left,
            int top
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.world == null) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;

        List<TeamScoreRenderData.TeamRow> rows =
                TeamScoreRenderData.getRows(client, true);

        if (rows.isEmpty()) {
            rows = List.of(
                    new TeamScoreRenderData.TeamRow(
                            net.minecraft.text.Text.literal("Красные")
                                    .formatted(net.minecraft.util.Formatting.RED),
                            8,
                            net.minecraft.util.Formatting.RED
                    ),

                    new TeamScoreRenderData.TeamRow(
                            net.minecraft.text.Text.literal("Синие")
                                    .formatted(net.minecraft.util.Formatting.BLUE),
                            8,
                            net.minecraft.util.Formatting.BLUE
                    )
            );
        }

        TeamScoreRenderData.Layout layout =
                TeamScoreRenderData.measure(textRenderer, rows);

        drawContext.fill(
                left,
                top,
                left + layout.width(),
                top + layout.height(),
                0x88000000
        );

        int y = top + TeamScoreRenderData.PADDING;

        for (TeamScoreRenderData.TeamRow row : rows) {

            drawContext.drawText(
                    textRenderer,
                    row.label(),
                    left + TeamScoreRenderData.PADDING,
                    y,
                    row.color().getColorValue() != null
                            ? row.color().getColorValue()
                            : 0xFFFFFF,
                    false
            );

            String scoreText = Integer.toString(row.value());

            int scoreX = left
                    + layout.width()
                    - TeamScoreRenderData.PADDING
                    - textRenderer.getWidth(scoreText);

            drawContext.drawText(
                    textRenderer,
                    scoreText,
                    scoreX,
                    y,
                    0xFFFFFF,
                    false
            );

            y += TeamScoreRenderData.LINE_HEIGHT;
        }

        Text turnText = Text.literal("Ход: лидера Красных");
        int turnWidth = textRenderer.getWidth(turnText) + 8;
        int turnY = top + layout.height() + 3;
        int turnHeight = 14;

        drawContext.fill(
                left,
                turnY,
                left + turnWidth,
                turnY + turnHeight,
                0x88000000
        );

        drawContext.drawText(
                textRenderer,
                turnText,
                left + 4,
                turnY + 3,
                0xFFFF5555,
                false
        );
    }
}