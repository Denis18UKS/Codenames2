package fable.codenames.client.renderer;

import fable.codenames.client.game.GameTimerClientState;
import fable.codenames.entity.XodKomandProjectorEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;

public class XodKomandProjectorEntityRenderer extends EntityRenderer<XodKomandProjectorEntity> {

    private final TextRenderer textRenderer;

    public XodKomandProjectorEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.textRenderer = context.getTextRenderer();
        this.shadowRadius = 0.0f;
    }

    private enum TurnState {
        RED_TEAM,
        RED_LEADER,
        BLUE_TEAM,
        BLUE_LEADER,
        NONE
    }

    private TurnState resolveState() {
        if (!GameTimerClientState.isActive()) {
            return TurnState.NONE;
        }

        String team = GameTimerClientState.getTeamName().toLowerCase();
        boolean leader = GameTimerClientState.canPassTurn();

        boolean red = team.contains("red") || team.contains("крас");
        boolean blue = team.contains("blue") || team.contains("син");

        if (red) return leader ? TurnState.RED_LEADER : TurnState.RED_TEAM;
        if (blue) return leader ? TurnState.BLUE_LEADER : TurnState.BLUE_TEAM;

        return TurnState.NONE;
    }

    private Text getText(TurnState state) {
        return switch (state) {
            case RED_TEAM -> Text.literal("Ход команды Красных");
            case RED_LEADER -> Text.literal("Ход лидера Красных");
            case BLUE_TEAM -> Text.literal("Ход команды Синих");
            case BLUE_LEADER -> Text.literal("Ход лидера Синих");
            case NONE -> Text.literal("Нет активного хода");
        };
    }

    private int getColor(TurnState state) {
        return switch (state) {
            case RED_TEAM, RED_LEADER -> 0xFFff5555;
            case BLUE_TEAM, BLUE_LEADER -> 0xFF55aaff;
            case NONE -> 0xFFFFFFFF;
        };
    }

    @Override
    public void render(
            XodKomandProjectorEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        TurnState state = resolveState();
        Text text = getText(state);
        int color = getColor(state);

        MinecraftClient client = MinecraftClient.getInstance();
        var camera = client.gameRenderer.getCamera();

        matrices.push();

        // позиция над сущностью
        matrices.translate(0.0, 1.2, 0.0);

        // BILLBOARD (всегда к камере)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        float scale = 0.025f;
        matrices.scale(-scale, -scale, scale);

        float x = -this.textRenderer.getWidth(text) / 2f;

        // ⭐ ВАЖНО: это правильный drawText с background как у nametag
        this.textRenderer.draw(
                text,
                x,
                0,
                color,
                false,
                matrices.peek().getPositionMatrix(),
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
        );

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public net.minecraft.util.Identifier getTexture(XodKomandProjectorEntity entity) {
        return null;
    }
}