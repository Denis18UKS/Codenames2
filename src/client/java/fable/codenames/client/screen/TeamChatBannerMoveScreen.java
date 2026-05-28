package fable.codenames.client.screen;

import fable.codenames.block.entity.TeamChatBlockEntity;
import fable.codenames.chat.TeamChatPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class TeamChatBannerMoveScreen extends Screen {
    private final BlockPos bannerPos;

    public TeamChatBannerMoveScreen(BlockPos bannerPos) {
        super(Text.literal("Team chat banner position"));
        this.bannerPos = bannerPos;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonSize = 24;
        int gap = 4;

        addDrawableChild(ButtonWidget.builder(Text.literal("^"), button -> move(0, 1))
                .dimensions(centerX - buttonSize / 2, centerY - buttonSize - gap, buttonSize, buttonSize)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("v"), button -> move(0, -1))
                .dimensions(centerX - buttonSize / 2, centerY + buttonSize + gap, buttonSize, buttonSize)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> move(-1, 0))
                .dimensions(centerX - buttonSize - gap, centerY, buttonSize, buttonSize)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> move(1, 0))
                .dimensions(centerX + gap, centerY, buttonSize, buttonSize)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> reset())
                .dimensions(centerX - 62, centerY + 60, 60, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(centerX + 2, centerY + 60, 60, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int y = this.height / 2 - 62;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, y, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, offsetText(), centerX, y + 14, 0xFFE0E0E0);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Buttons: 1 px, arrows: 1 px, Shift+arrows: 8 px"), centerX, this.height / 2 + 88, 0xFFA0A0A0);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int step = Screen.hasShiftDown() ? 8 : 1;
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            move(-step, 0);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            move(step, 0);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            move(0, step);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            move(0, -step);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            reset();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private Text offsetText() {
        BlockEntity blockEntity = this.client != null && this.client.world != null
                ? this.client.world.getBlockEntity(this.bannerPos)
                : null;
        if (blockEntity instanceof TeamChatBlockEntity teamChatBlockEntity) {
            return Text.literal("X: " + teamChatBlockEntity.getBannerOffsetXPixels()
                    + " px, Y: " + teamChatBlockEntity.getBannerOffsetYPixels() + " px");
        }
        return Text.literal("Banner is not loaded");
    }

    private void move(int deltaX, int deltaY) {
        send(deltaX, deltaY, false);
    }

    private void reset() {
        send(0, 0, true);
    }

    private void send(int deltaX, int deltaY, boolean reset) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(this.bannerPos);
        buf.writeVarInt(deltaX);
        buf.writeVarInt(deltaY);
        buf.writeBoolean(reset);
        ClientPlayNetworking.send(TeamChatPackets.MOVE_BANNER, buf);
    }
}
