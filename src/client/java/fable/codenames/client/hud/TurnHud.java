package fable.codenames.client.hud;

import fable.codenames.client.game.GameTimerClientState;
import fable.codenames.game.CodenamesPhase;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class TurnHud {
    private TurnHud() {}

    public static void init() {
        HudRenderCallback.EVENT.register((context, tickDelta) -> render(context));
    }

    private static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden) {
            return;
        }

        if (!GameTimerClientState.isActive()) {
            return;
        }

        String team = GameTimerClientState.getTeamName().toLowerCase();
        CodenamesPhase phase = GameTimerClientState.getPhase();

        boolean red = team.contains("red") || team.contains("крас");
        boolean blue = team.contains("blue") || team.contains("син");

        if (!red && !blue) {
            return;
        }

        boolean leaderPhase = (phase == CodenamesPhase.WAITING_CLUE);

        String textStr;
        int color;

        if (red) {
            textStr = leaderPhase ? "Ход: лидера команды: Красных" : "Ход команды: Красных";
            color = 0xFFFF5555;
        } else {
            textStr = leaderPhase ? "Ход: лидера команды: Синих" : "Ход команды: Синих";
            color = 0xFF55AAFF;
        }

        Text text = Text.literal(textStr);
        int textWidth = client.textRenderer.getWidth(text);
        int x = (context.getScaledWindowWidth() - textWidth) / 2;
        int y = 5;

        context.fill(x - 6, y - 4, x + textWidth + 6, y + 11, 0x80000000);
        context.drawTextWithShadow(client.textRenderer, text, x, y, color);
    }
}