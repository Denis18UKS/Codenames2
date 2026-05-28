package fable.codenames.client.screen;

import fable.codenames.client.hud.TeamHudState;
import fable.codenames.client.hud.TeamListHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class TeamHudEditorScreen extends Screen {
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public TeamHudEditorScreen() {
        super(Text.literal("Codenames HUD editor"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int x = TeamHudState.getX();
        int y = TeamHudState.getY();
        TeamListHud.renderPreview(context, x, y);
        context.drawText(this.textRenderer, "Перетащи счетчик мышкой. ESC - сохранить и выйти.", 12, this.height - 18, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = true;
            dragOffsetX = (int) mouseX - TeamHudState.getX();
            dragOffsetY = (int) mouseY - TeamHudState.getY();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            TeamHudState.setPosition((int) mouseX - dragOffsetX, (int) mouseY - dragOffsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
            TeamHudState.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        TeamHudState.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
