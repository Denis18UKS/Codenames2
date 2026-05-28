package fable.codenames.client.screen;

import fable.codenames.client.chat.TeamChatMessengerRenderer;
import fable.codenames.client.chat.TeamChatTextLayout;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TeamChatTextLayoutScreen extends Screen {
    public TeamChatTextLayoutScreen() {
        super(Text.literal("Team chat text layout"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = this.height / 2 + 58;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> TeamChatTextLayout.move(-1, 0))
                .dimensions(centerX - 64, top, 28, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> TeamChatTextLayout.move(1, 0))
                .dimensions(centerX + 36, top, 28, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("^"), button -> TeamChatTextLayout.move(0, -1))
                .dimensions(centerX - 14, top - 24, 28, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("v"), button -> TeamChatTextLayout.move(0, 1))
                .dimensions(centerX - 14, top + 24, 28, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("A-"), button -> TeamChatTextLayout.scale(-0.05F))
                .dimensions(centerX - 64, top + 50, 38, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("A+"), button -> TeamChatTextLayout.scale(0.05F))
                .dimensions(centerX + 26, top + 50, 38, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Side"), button -> TeamChatTextLayout.toggleSide())
                .dimensions(centerX - 44, top + 74, 88, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> TeamChatTextLayout.reset())
                .dimensions(centerX - 34, top + 98, 68, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        int panelX = (this.width - TeamChatMessengerRenderer.PANEL_WIDTH) / 2;
        int y = this.height / 2 - 56;
        TeamChatMessengerRenderer.drawScreenBubble(context, this.textRenderer, panelX, y,
                TeamChatMessengerRenderer.buildPreviewMessage(this.textRenderer, "Pachycephalosaur 1", "Red", false));
        y += 28;
        TeamChatMessengerRenderer.drawScreenBubble(context, this.textRenderer, panelX, y,
                TeamChatMessengerRenderer.buildPreviewMessage(this.textRenderer, "Pachycephalosaur unlimited", "Blue", true));
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Side: " + (TeamChatTextLayout.activeSide() == TeamChatTextLayout.Side.LEFT ? "left" : "right")
                        + "  X: " + TeamChatTextLayout.textX(TeamChatTextLayout.activeSide() == TeamChatTextLayout.Side.RIGHT)
                        + "  Y: " + TeamChatTextLayout.textY()
                        + "  Scale: " + String.format("%.2f", TeamChatTextLayout.textScale())),
                this.width / 2,
                this.height / 2 + 34,
                0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
