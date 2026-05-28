package fable.codenames.client.chat;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;

public final class ChatHoverHintRenderer {

    private static float hoverProgress = 0.0f;
    private static final float SPEED = 0.08f;

    public static void init() {
        HudRenderCallback.EVENT.register((context, tickDelta) -> renderHud(context, tickDelta));
    }

    private static void renderHud(DrawContext context, float tickDelta) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return;

        boolean hovering = TeamChatClientState.canSend()
                && !TeamChatClientState.hasActiveBannerInput()
                && isLookingAtTeamChat(client);

        // =========================
        // FADE
        // =========================
        if (hovering)
            hoverProgress += SPEED;
        else
            hoverProgress -= SPEED;

        hoverProgress = Math.max(0.0f, Math.min(1.0f, hoverProgress));

        if (hoverProgress <= 0.01f)
            return;

        int alpha = (int) (hoverProgress * 180); // 180 = более “ванильная” плотность

        String message = "Нажмите ПКМ, чтобы писать в командный чат";

        int textWidth = client.textRenderer.getWidth(message);
        int textHeight = client.textRenderer.fontHeight;

        int paddingX = 10;
        int paddingY = 6;

        int boxWidth = textWidth + paddingX * 2;
        int boxHeight = textHeight + paddingY * 2;

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();

        int x = (screenW - boxWidth) / 2;
        int y = screenH - 60;

        // =========================
        // BACKGROUND (плашка)
        // =========================
        int bgColor = (alpha << 24) | 0x000000;

        context.fill(
                x,
                y,
                x + boxWidth,
                y + boxHeight,
                bgColor);

        // =========================
        // TEXT
        // =========================
        int textColor = (alpha << 24) | 0xFFFFFF;

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(message),
                x + paddingX,
                y + paddingY,
                textColor);
    }

    private static boolean isLookingAtTeamChat(MinecraftClient client) {

        if (!(client.crosshairTarget instanceof BlockHitResult hit))
            return false;

        return client.world.getBlockState(hit.getBlockPos())
                .getBlock() instanceof fable.codenames.block.TeamChatBlock;
    }
}