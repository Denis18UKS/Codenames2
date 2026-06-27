package fable.codenames.client.renderer;

import fable.codenames.block.TeamChatBlock;
import fable.codenames.block.entity.TeamChatBlockEntity;
import fable.codenames.client.chat.TeamChatClientState;
import fable.codenames.client.chat.TeamChatMessengerRenderer;
import fable.codenames.client.chat.TeamChatVisuals;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.List;

public class TeamChatBlockEntityRenderer implements BlockEntityRenderer<TeamChatBlockEntity> {
    private static final float BASE_OFFSET_X = 8.0F / 16.0F;
    private static final float HEIGHT = 4.0F;
    private static final float WIDTH = 2.0F;
    private static final float CONTENT_Z = 0.02F;
    private static final float WALL_Z = -0.492F;
    private static final int FULL_BRIGHT_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;

    public TeamChatBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(TeamChatBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState state = entity.getCachedState();
        if (!(state.getBlock() instanceof TeamChatBlock)) {
            return;
        }

        Direction facing = state.get(TeamChatBlock.FACING);
        matrices.push();
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationFor(facing)));
        matrices.translate(BASE_OFFSET_X + entity.getBannerOffsetXPixels() / 16.0F, entity.getBannerOffsetYPixels() / 16.0F, 0.0F);
        matrices.translate(-WIDTH / 2.0F, 0.0F, WALL_Z);

        drawPanel(matrices, vertexConsumers, entity.getTeamName());
        drawMessages(matrices, vertexConsumers);
        drawInput(matrices, vertexConsumers, entity);

        matrices.pop();
    }

    private static void drawPanel(MatrixStack matrices, VertexConsumerProvider vertexConsumers, String teamName) {
        String visibleTeam = teamName == null || teamName.isBlank() ? TeamChatClientState.getCurrentTeam() : teamName;
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getText(TeamChatVisuals.backgroundTexture(visibleTeam)));
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        addVertex(consumer, positionMatrix, 0.0F, HEIGHT, 0.0F, 0.0F, 0.0F);
        addVertex(consumer, positionMatrix, WIDTH, HEIGHT, 0.0F, 1.0F, 0.0F);
        addVertex(consumer, positionMatrix, WIDTH, 0.0F, 0.0F, 1.0F, 1.0F);
        addVertex(consumer, positionMatrix, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
    }

    private static void drawMessages(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        List<TeamChatMessengerRenderer.RenderedMessage> messages = TeamChatMessengerRenderer.buildMessages(textRenderer, TeamChatClientState.getMessages());
        if (messages.isEmpty()) {
            return;
        }

        matrices.push();
        matrices.translate(0.0F, HEIGHT, CONTENT_Z);
        matrices.scale(WIDTH / TeamChatMessengerRenderer.PANEL_WIDTH, -HEIGHT / TeamChatMessengerRenderer.PANEL_HEIGHT, 1.0F);

        int firstVisible = TeamChatMessengerRenderer.firstVisibleIndexFromBottom(messages);
        List<TeamChatMessengerRenderer.RenderedMessage> visible = messages.subList(firstVisible, messages.size());
        int totalHeight = TeamChatMessengerRenderer.totalHeight(visible);
        int y = Math.max(TeamChatMessengerRenderer.CHAT_TOP, TeamChatMessengerRenderer.CHAT_BOTTOM - 6 - totalHeight);

        for (TeamChatMessengerRenderer.RenderedMessage message : visible) {
            TeamChatMessengerRenderer.drawWorldBubble(matrices, vertexConsumers, textRenderer, y, message);
            y += message.height();
        }

        matrices.pop();
    }

    private static void drawInput(MatrixStack matrices, VertexConsumerProvider vertexConsumers, TeamChatBlockEntity entity) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        matrices.push();
        matrices.translate(0.0F, HEIGHT, CONTENT_Z + 0.002F);
        matrices.scale(WIDTH / TeamChatMessengerRenderer.PANEL_WIDTH, -HEIGHT / TeamChatMessengerRenderer.PANEL_HEIGHT, 1.0F);
        boolean active = TeamChatClientState.isBannerInputActive(entity.getPos());
        List<TeamChatMessengerRenderer.RenderedMessage> messages = TeamChatMessengerRenderer.buildMessages(textRenderer, TeamChatClientState.getMessages());
        TeamChatMessengerRenderer.drawWorldInput(
                matrices,
                vertexConsumers,
                textRenderer,
                messages,
                active ? TeamChatClientState.getDraft() : "",
                active,
                TeamChatClientState.canSend());
        matrices.pop();
    }

    private static float rotationFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST -> -90.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f positionMatrix,
                                  float x, float y, float z, float u, float v) {
        consumer.vertex(positionMatrix, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .light(FULL_BRIGHT_LIGHT)
                .next();
    }
}
