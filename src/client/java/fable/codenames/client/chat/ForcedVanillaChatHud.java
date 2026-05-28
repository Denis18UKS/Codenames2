package fable.codenames.client.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ForcedVanillaChatHud {
    private static final long VISIBLE_MS = 6000L;
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private ForcedVanillaChatHud() {
    }

    public static void add(Text message) {
        ENTRIES.add(new Entry(message, System.currentTimeMillis()));
        while (ENTRIES.size() > 8) {
            ENTRIES.remove(0);
        }
    }

    public static void render(DrawContext context) {
        if (ENTRIES.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        ENTRIES.removeIf(entry -> now - entry.createdAtMs > VISIBLE_MS);
        if (ENTRIES.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int lineHeight = 10;
        int y = context.getScaledWindowHeight() - 40 - (ENTRIES.size() - 1) * lineHeight;
        for (Entry entry : ENTRIES) {
            int width = textRenderer.getWidth(entry.message);
            context.fill(2, y - 1, 6 + width, y + 9, 0x66000000);
            context.drawText(textRenderer, entry.message, 4, y, 0xFFFFFFFF, true);
            y += lineHeight;
        }
    }

    private record Entry(Text message, long createdAtMs) {
    }
}
