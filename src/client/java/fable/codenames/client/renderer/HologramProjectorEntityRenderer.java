package fable.codenames.client.renderer;

import fable.codenames.client.game.GameTimerClientState;
import fable.codenames.client.hud.TeamScoreRenderData;
import fable.codenames.entity.HologramProjectorEntity;
import fable.codenames.game.CodenamesPhase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.List;

public class HologramProjectorEntityRenderer extends EntityRenderer<HologramProjectorEntity> {

    private static final Identifier TEXTURE =
            SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;

    private final TextRenderer textRenderer;

    public HologramProjectorEntityRenderer(
            EntityRendererFactory.Context context
    ) {
        super(context);

        this.textRenderer = context.getTextRenderer();
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(
            HologramProjectorEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {

        MinecraftClient client = MinecraftClient.getInstance();

        List<TeamScoreRenderData.TeamRow> rows =
                TeamScoreRenderData.getRows(client, false);

        boolean showPassTurn =
                GameTimerClientState.isActive()
                        && GameTimerClientState.getPhase() == CodenamesPhase.GUESSING
                        && GameTimerClientState.canPassTurn();

        if (rows.isEmpty() && !showPassTurn) {
            return;
        }

        TeamScoreRenderData.Layout measured =
                rows.isEmpty()
                        ? new TeamScoreRenderData.Layout(0, 0)
                        : TeamScoreRenderData.measure(
                        this.textRenderer,
                        rows
                );

        Text passTurn = Text.literal("Передать ход");

        int passWidth =
                showPassTurn
                        ? this.textRenderer.getWidth(passTurn)
                        + TeamScoreRenderData.PADDING * 2
                        : 0;

        int passHeight =
                showPassTurn
                        ? TeamScoreRenderData.LINE_HEIGHT
                        : 0;

        int width = Math.max(measured.width(), passWidth);

        int height =
                rows.isEmpty()
                        ? TeamScoreRenderData.PADDING * 2 + passHeight
                        : measured.height() + passHeight;

        matrices.push();

        matrices.translate(0.0, 1, 0.0);

        matrices.multiply(
                net.minecraft.util.math.RotationAxis.POSITIVE_Y
                        .rotationDegrees(180.0F - entity.getYaw())
        );

        matrices.scale(-0.025f, -0.025f, 0.025f);

        matrices.translate(-(width / 2.0f), 0.0f, 0.0f);

        drawBackground(
                matrices,
                vertexConsumers,
                width,
                height
        );

        matrices.translate(0.0f, 0.0f, -0.01f);

        // УБРАН TITLE

        int y = TeamScoreRenderData.PADDING;

        for (TeamScoreRenderData.TeamRow row : rows) {

            int teamColor =
                    row.color().getColorValue() != null
                            ? 0xFF000000 | row.color().getColorValue()
                            : 0xFFFFFFFF;

            drawTextLine(
                    matrices,
                    vertexConsumers,
                    row.label(),
                    TeamScoreRenderData.PADDING,
                    y,
                    teamColor
            );

            String scoreText = Integer.toString(row.value());

            int scoreX =
                    width
                            - TeamScoreRenderData.PADDING
                            - this.textRenderer.getWidth(scoreText);

            drawTextLine(
                    matrices,
                    vertexConsumers,
                    Text.literal(scoreText),
                    scoreX,
                    y,
                    0xFFFFFFFF
            );

            y += TeamScoreRenderData.LINE_HEIGHT;
        }

        if (showPassTurn) {

            drawTextLine(
                    matrices,
                    vertexConsumers,
                    passTurn,
                    TeamScoreRenderData.PADDING,
                    y,
                    teamColor(GameTimerClientState.getTeamName())
            );
        }

        matrices.pop();

        super.render(
                entity,
                yaw,
                tickDelta,
                matrices,
                vertexConsumers,
                light
        );
    }

    private void drawBackground(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int width,
            int height
    ) {

        Matrix4f matrix =
                matrices.peek().getPositionMatrix();

        var consumer =
                vertexConsumers.getBuffer(
                        RenderLayer.getTextBackground()
                );

        float left = 0;
        float top = 0;
        float right = width;
        float bottom = height;

        int color = 0x6A000000;

        float a = (color >> 24 & 255) / 255.0f;
        float r = (color >> 16 & 255) / 255.0f;
        float g = (color >> 8 & 255) / 255.0f;
        float b = (color & 255) / 255.0f;

        consumer.vertex(matrix, left, bottom, 0)
                .color(r, g, b, a)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .next();

        consumer.vertex(matrix, right, bottom, 0)
                .color(r, g, b, a)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .next();

        consumer.vertex(matrix, right, top, 0)
                .color(r, g, b, a)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .next();

        consumer.vertex(matrix, left, top, 0)
                .color(r, g, b, a)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .next();
    }

    private void drawTextLine(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            Text text,
            int x,
            int y,
            int color
    ) {

        this.textRenderer.draw(
                text,
                x,
                y,
                color,
                false,
                matrices.peek().getPositionMatrix(),
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                15728880
        );
    }

    private static int teamColor(String teamName) {

        String normalized =
                teamName.toLowerCase(java.util.Locale.ROOT);

        if (normalized.contains("blue")
                || normalized.contains("син")) {

            return 0xFF22D3EE;
        }

        if (normalized.contains("red")
                || normalized.contains("крас")) {

            return 0xFFFF3333;
        }

        return 0xFFFFFFFF;
    }

    @Override
    public Identifier getTexture(
            HologramProjectorEntity entity
    ) {
        return TEXTURE;
    }
}