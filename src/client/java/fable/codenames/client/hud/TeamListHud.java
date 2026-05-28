package fable.codenames.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

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
    }
}