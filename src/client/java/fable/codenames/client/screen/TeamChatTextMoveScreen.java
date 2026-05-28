package fable.codenames.client.screen;

import fable.codenames.client.chat.TeamChatMessengerRenderer;
import fable.codenames.client.chat.TeamChatTextLayout;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TeamChatTextMoveScreen extends Screen {
    private static final int PREVIEW_GAP = 28;
    private boolean dragging;
    private TeamChatTextLayout.Side dragSide = TeamChatTextLayout.Side.LEFT;

    public TeamChatTextMoveScreen() {
        super(Text.literal("Team chat text move"));
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
        addDrawableChild(ButtonWidget.builder(Text.literal("Center"), button -> centerText(TeamChatTextLayout.activeSide()))
                .dimensions(centerX - 44, top + 74, 88, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(centerX - 30, top + 98, 60, 20)
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
        y += PREVIEW_GAP;
        TeamChatMessengerRenderer.drawScreenBubble(context, this.textRenderer, panelX, y,
                TeamChatMessengerRenderer.buildPreviewMessage(this.textRenderer, "Pachycephalosaur unlimited", "Blue", true));

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Side: " + (TeamChatTextLayout.activeSide() == TeamChatTextLayout.Side.LEFT ? "left" : "right")
                        + "  X: " + TeamChatTextLayout.textX(TeamChatTextLayout.activeSide() == TeamChatTextLayout.Side.RIGHT)
                        + "  Y: " + TeamChatTextLayout.textY()),
                this.width / 2,
                this.height / 2 + 34,
                0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("ЛКМ по облачку: двигать | Shift + ЛКМ: центр X"),
                this.width / 2,
                this.height / 2 + 46,
                0xA0D8FF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.textRenderer != null) {
            int panelX = (this.width - TeamChatMessengerRenderer.PANEL_WIDTH) / 2;
            int leftY = this.height / 2 - 56;
            int rightY = leftY + PREVIEW_GAP;

            TeamChatTextLayout.Side side = null;
            if (insideBubble(mouseX, mouseY, panelX, leftY)) {
                side = TeamChatTextLayout.Side.LEFT;
            } else if (insideBubble(mouseX, mouseY, panelX, rightY)) {
                side = TeamChatTextLayout.Side.RIGHT;
            }

            if (side != null) {
                TeamChatTextLayout.setActiveSide(side);
                this.dragSide = side;
                if (hasShiftDown()) {
                    centerText(side);
                } else {
                    this.dragging = true;
                    updateTextPositionFromMouse(mouseX, mouseY, side == TeamChatTextLayout.Side.RIGHT ? rightY : leftY);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.dragging && button == 0) {
            int previewY = this.dragSide == TeamChatTextLayout.Side.RIGHT ? this.height / 2 - 56 + PREVIEW_GAP : this.height / 2 - 56;
            updateTextPositionFromMouse(mouseX, mouseY, previewY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void centerText(TeamChatTextLayout.Side side) {
        String preview = side == TeamChatTextLayout.Side.LEFT ? "Pachycephalosaur 1" : "Pachycephalosaur unlimited";
        int textWidth = this.textRenderer.getWidth(preview);
        float scale = TeamChatTextLayout.textScale();
        int centeredX = Math.round((TeamChatMessengerRenderer.PANEL_WIDTH - (textWidth * scale)) / (2.0f * scale));
        TeamChatTextLayout.setPosition(side, centeredX, TeamChatTextLayout.textY());
    }

    private void updateTextPositionFromMouse(double mouseX, double mouseY, int previewY) {
        int relativeX = (int) Math.round((mouseX - ((this.width - TeamChatMessengerRenderer.PANEL_WIDTH) / 2)) / TeamChatTextLayout.textScale());
        int relativeY = (int) Math.round((mouseY - previewY) / TeamChatTextLayout.textScale());
        TeamChatTextLayout.setPosition(this.dragSide, relativeX, relativeY);
    }

    private boolean insideBubble(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + TeamChatMessengerRenderer.PANEL_WIDTH
                && mouseY >= y && mouseY <= y + TeamChatMessengerRenderer.PANEL_HEIGHT;
    }
}
