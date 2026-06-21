package fable.codenames.client.chat;

import fable.codenames.chat.TeamChatMessage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class TeamChatMessengerRenderer {
    public static final Identifier CHAT_BANNER_FONT = new Identifier("codenames", "chat_banner");
    public static final Style CHAT_STYLE = Style.EMPTY.withFont(CHAT_BANNER_FONT);
    public static final int PANEL_WIDTH = 124;
    public static final int PANEL_HEIGHT = 248;
    public static final int CHAT_TOP = 18;
    public static final int CHAT_BOTTOM = PANEL_HEIGHT - 42;

    private static final int BUBBLE_TEXTURE_WIDTH = 36;
    private static final int BUBBLE_TEXTURE_HEIGHT = 10;
    private static final int BUBBLE_HEIGHT = 19;
    private static final int BUBBLE_MIN_WIDTH = 42;
    private static final int BUBBLE_LANE_LEFT = 4;
    private static final int BUBBLE_LANE_RIGHT = PANEL_WIDTH - 4;
    private static final int BUBBLE_MAX_WIDTH = BUBBLE_LANE_RIGHT - BUBBLE_LANE_LEFT;
    private static final int LEFT_BUBBLE_RIGHT_PADDING = 4;
    private static final int RIGHT_BUBBLE_RIGHT_PADDING = 16;
    private static final int BUBBLE_GAP = 6;
    private static final float WORLD_TEXT_Z_OFFSET = 0.002F;

    // Хранилище для сообщений (чтобы метод drawWorldInput не требовал лишнего аргумента)
    private static List<RenderedMessage> currentRenderedMessages = new ArrayList<>();

    private TeamChatMessengerRenderer() {
    }

    public static List<RenderedMessage> buildMessages(TextRenderer textRenderer, List<TeamChatMessage> messages) {
        List<RenderedMessage> rendered = new ArrayList<>();
        for (TeamChatMessage message : messages) {
            boolean own = TeamChatClientState.isOwnMessage(message);
            rendered.add(buildMessage(textRenderer, message.content(), message.teamName(), own, message.sentAtMillis()));
        }
        // Сохраняем сообщения для использования в drawWorldInput
        currentRenderedMessages = rendered;
        return rendered;
    }

    public static RenderedMessage buildPreviewMessage(TextRenderer textRenderer, String content, String teamName, boolean own) {
        return buildMessage(textRenderer, content, teamName, own, System.currentTimeMillis() - 1000L);
    }

    private static RenderedMessage buildMessage(TextRenderer textRenderer, String content, String teamName, boolean own, long sentAtMillis) {
        Text lineText = styled(content);
        int textWidth = textRenderer.getWidth(lineText);
        float textScale = textScale(textWidth, own);
        int bubbleWidth = bubbleWidth(textWidth, textScale, own);
        int x = bubbleX(bubbleWidth, own);
        return new RenderedMessage(lineText, x, bubbleWidth, BUBBLE_HEIGHT, textScale, own, teamName, sentAtMillis);
    }

    public static boolean fitsSingleBubble(TextRenderer textRenderer, String content) {
        return !content.isBlank()
                && !content.contains("\n")
                && textRenderer.getWidth(styled(content.trim())) <= maxInputTextWidth(false)
                && textRenderer.getWidth(styled(content.trim())) <= maxInputTextWidth(true);
    }

    public static int maxTextWidth() {
        return Math.min(maxTextWidth(false), maxTextWidth(true));
    }

    private static int maxTextWidth(boolean own) {
        return BUBBLE_MAX_WIDTH - TeamChatTextLayout.textX(own) - textRightPadding(own);
    }

    private static int maxInputTextWidth(boolean own) {
        return (int) Math.floor(maxTextWidth(own) / TeamChatTextLayout.minTextScale());
    }

    private static float textScale(int textWidth, boolean own) {
        if (textWidth <= 0) {
            return TeamChatTextLayout.textScale();
        }
        float configuredScale = TeamChatTextLayout.textScale();
        float fitScale = maxTextWidth(own) / (float) textWidth;
        return Math.max(TeamChatTextLayout.minTextScale(), Math.min(configuredScale, fitScale));
    }

    private static int bubbleWidth(int textWidth, float textScale, boolean own) {
        int scaledTextWidth = (int) Math.ceil(textWidth * textScale);
        return Math.min(BUBBLE_MAX_WIDTH, Math.max(BUBBLE_MIN_WIDTH, scaledTextWidth + TeamChatTextLayout.textX(own) + textRightPadding(own)));
    }

    private static int textRightPadding(boolean own) {
        return own ? RIGHT_BUBBLE_RIGHT_PADDING : LEFT_BUBBLE_RIGHT_PADDING;
    }

    private static int bubbleX(int bubbleWidth, boolean own) {
        int preferred = own ? PANEL_WIDTH - bubbleWidth - 10 : 10;
        return clamp(preferred, BUBBLE_LANE_LEFT, BUBBLE_LANE_RIGHT - bubbleWidth);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int totalHeight(List<RenderedMessage> messages) {
        return messages.stream().mapToInt(RenderedMessage::height).sum();
    }

    public static int firstVisibleIndexFromBottom(List<RenderedMessage> messages) {
        int height = 0;
        for (int i = messages.size() - 1; i >= 0; i--) {
            height += messages.get(i).height();
            if (height > CHAT_BOTTOM - CHAT_TOP - 6) {
                return i + 1;
            }
        }
        return 0;
    }

    public static void drawScreenBubble(DrawContext context, TextRenderer textRenderer, int panelX, int y, RenderedMessage message) {
        float progress = animationProgress(message.sentAtMillis());
        float scale = animationScale(progress);
        int left = panelX + message.x();

        context.getMatrices().push();
        context.getMatrices().translate(left + message.bubbleWidth() / 2.0F, y + message.bubbleHeight(), 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.getMatrices().translate(-(left + message.bubbleWidth() / 2.0F), -(y + message.bubbleHeight()), 0.0F);
        context.getMatrices().translate(0.0F, animationYOffset(progress), 0.0F);
        context.drawTexture(
                TeamChatVisuals.bubbleTexture(message.teamName(), message.own()),
                left,
                y,
                message.bubbleWidth(),
                message.bubbleHeight(),
                0.0F,
                0.0F,
                BUBBLE_TEXTURE_WIDTH,
                BUBBLE_TEXTURE_HEIGHT,
                BUBBLE_TEXTURE_WIDTH,
                BUBBLE_TEXTURE_HEIGHT);
        drawScreenText(context, textRenderer, left, y, message);
        context.getMatrices().pop();
    }

    public static void drawWorldBubble(MatrixStack matrices, VertexConsumerProvider vertexConsumers, TextRenderer textRenderer,
                                       int y, RenderedMessage message, int light) {
        float progress = animationProgress(message.sentAtMillis());
        float scale = animationScale(progress);

        // Инверсия Y для мира (в мире Y растет вверх, в GUI - вниз)
        int worldY = -y - message.bubbleHeight();
        int x = message.x();

        matrices.push();
        matrices.translate(x + message.bubbleWidth() / 2.0F, worldY + message.bubbleHeight() / 2.0F, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        matrices.translate(-(x + message.bubbleWidth() / 2.0F), -(worldY + message.bubbleHeight() / 2.0F), 0.0F);
        matrices.translate(0.0F, animationYOffset(progress), 0.0F);

        // 1. Рисуем текстуру пузыря
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TeamChatVisuals.bubbleTexture(message.teamName(), message.own())));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();

        drawTexturedQuad(consumer, matrix, normalMatrix, 
                x, worldY, 
                x + message.bubbleWidth(), worldY + message.bubbleHeight(), 
                light);

        // 2. Рисуем текст
        matrices.push();
        matrices.translate(0.0F, 0.0F, 0.01F); // Чуть выдвигаем текст вперед, чтобы не мерцал на фоне

        int textX = x + centeredTextX(textRenderer, message);
        int textY = worldY + TeamChatTextLayout.textY();

        matrices.translate(textX, textY, 0.0F);
        matrices.scale(message.textScale(), message.textScale(), 1.0F);

        textRenderer.draw(
                message.lineText(),
                0, 0,
                0xFF1E1E1E,
                false,
                matrices.peek().getPositionMatrix(),
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
        );
        matrices.pop();
        matrices.pop();
    }

    // ============================================================
    // ПОЛЕ ВВОДА В МИРЕ (С ОТСТУПОМ В 4 БЛОКА ВНИЗ)
    // ============================================================
    public static void drawWorldInput(MatrixStack matrices, VertexConsumerProvider vertexConsumers, TextRenderer textRenderer,
                                      String draft, boolean active, boolean canSend, int light) {
        
        // Считаем высоту всех сообщений
        int totalMessagesHeight = currentRenderedMessages.stream().mapToInt(RenderedMessage::height).sum();
        
        // Вычисляем позицию под последним сообщением. 
        // ДОБАВЛЕНО "- 4" в конце для смещения на 4 блока вниз в 3D пространстве.
        int localY = - (CHAT_TOP + totalMessagesHeight + 6) + 248; 

        int x = 12;
        int width = PANEL_WIDTH - 24;
        int height = 18;
        int color = active ? 0xEE050505 : 0xAA050505;

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(new Identifier("textures/misc/white.png")));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix3f normalMatrix = matrices.peek().getNormalMatrix();

        float a = ((color >> 24) & 255) / 255.0F;
        float r = ((color >> 16) & 255) / 255.0F;
        float g = ((color >> 8) & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        // Рисуем фон поля ввода (безопасный VertexConsumer)
        consumer.vertex(matrix, x, localY + height, 0.0F).color(r, g, b, a).texture(0.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).next();
        consumer.vertex(matrix, x + width, localY + height, 0.0F).color(r, g, b, a).texture(1.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).next();
        consumer.vertex(matrix, x + width, localY, 0.0F).color(r, g, b, a).texture(1.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).next();
        consumer.vertex(matrix, x, localY, 0.0F).color(r, g, b, a).texture(0.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normalMatrix, 0.0F, 0.0F, 1.0F).next();

        if (!canSend) return;

        String text = draft == null ? "" : draft;
        if (active && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            text += "_";
        }
        Text line = clipToWidth(textRenderer, text, width - 12);

        // Рисуем текст внутри поля ввода
        matrices.push();
        matrices.translate(0.0F, 0.0F, 0.01F); 
        textRenderer.draw(
                line,
                x + 6, localY + 5,
                0xFFFFFFFF,
                false,
                matrices.peek().getPositionMatrix(),
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
        );
        matrices.pop();
    }

    private static void drawTexturedQuad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                                         float x1, float y1, float x2, float y2, int light) {
        consumer.vertex(matrix, x1, y2, 0.0F).color(255, 255, 255, 255).texture(0.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal, 0.0F, 0.0F, 1.0F).next();
        consumer.vertex(matrix, x2, y2, 0.0F).color(255, 255, 255, 255).texture(1.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal, 0.0F, 0.0F, 1.0F).next();
        consumer.vertex(matrix, x2, y1, 0.0F).color(255, 255, 255, 255).texture(1.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal, 0.0F, 0.0F, 1.0F).next();
        consumer.vertex(matrix, x1, y1, 0.0F).color(255, 255, 255, 255).texture(0.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal, 0.0F, 0.0F, 1.0F).next();
    }

    private static void drawScreenText(DrawContext context, TextRenderer textRenderer, int left, int y, RenderedMessage message) {
        int textX = left + centeredTextX(textRenderer, message);
        int textY = y + TeamChatTextLayout.textY();
        context.getMatrices().push();
        context.getMatrices().translate(textX, textY, 0.0F);
        context.getMatrices().scale(message.textScale(), message.textScale(), 1.0F);
        context.drawText(textRenderer, message.lineText(), 0, 0, 0xFF1E1E1E, false);
        context.getMatrices().pop();
    }

    private static int centeredTextX(TextRenderer textRenderer, RenderedMessage message) {
        int textWidth = (int) Math.ceil(textRenderer.getWidth(message.lineText()) * message.textScale());
        int leftPadding = TeamChatTextLayout.textX(message.own());
        int rightPadding = textRightPadding(message.own());
        int availableWidth = Math.max(1, message.bubbleWidth() - leftPadding - rightPadding);
        return leftPadding + Math.max(0, (availableWidth - textWidth) / 2);
    }

    public static Text styled(String value) {
        return Text.literal(value).setStyle(CHAT_STYLE);
    }

    private static Text clipToWidth(TextRenderer textRenderer, String content, int maxWidth) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            String candidate = builder.toString() + content.charAt(i);
            Text withEllipsis = styled(i < content.length() - 1 ? candidate + "..." : candidate);
            if (textRenderer.getWidth(withEllipsis) > maxWidth) {
                return styled(builder.isEmpty() ? "..." : builder + "...");
            }
            builder.append(content.charAt(i));
        }
        return styled(builder.toString());
    }

    private static float animationProgress(long sentAtMillis) {
        long age = System.currentTimeMillis() - sentAtMillis;
        float linear = Math.max(0.0F, Math.min(1.0F, age / 420.0F));
        return 1.0F - (1.0F - linear) * (1.0F - linear);
    }

    private static float animationScale(float progress) {
        return 0.82F + 0.18F * progress;
    }

    private static float animationYOffset(float progress) {
        return (1.0F - progress) * 12.0F;
    }

    public record RenderedMessage(Text lineText, int x, int bubbleWidth, int bubbleHeight, float textScale, boolean own, String teamName, long sentAtMillis) {
        public int height() {
            return this.bubbleHeight + BUBBLE_GAP;
        }
    }
}