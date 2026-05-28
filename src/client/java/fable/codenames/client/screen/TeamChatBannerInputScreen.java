package fable.codenames.client.screen;

import fable.codenames.chat.TeamChatPackets;
import fable.codenames.client.chat.TeamChatClientState;
import fable.codenames.client.chat.TeamChatMessengerRenderer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class TeamChatBannerInputScreen extends Screen {
    private final BlockPos bannerPos;

    public TeamChatBannerInputScreen(BlockPos bannerPos) {
        super(TeamChatMessengerRenderer.styled("Командный чат"));
        this.bannerPos = bannerPos;
    }

    @Override
    protected void init() {
        TeamChatClientState.startBannerInput(this.bannerPos);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // The chat is rendered on the world banner itself; this screen only captures typing.
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!TeamChatClientState.canSend()) {
            return true;
        }
        String draft = TeamChatClientState.getDraft();
        if (draft.length() >= 32767 || Character.isISOControl(chr)) {
            return true;
        }
        String next = draft + chr;
        if (TeamChatMessengerRenderer.fitsSingleBubble(MinecraftClient.getInstance().textRenderer, next)) {
            TeamChatClientState.setDraft(next);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            close();
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            sendCurrentMessage();
            return true;
        }
        if (keyCode == 259) {
            String draft = TeamChatClientState.getDraft();
            if (!draft.isEmpty()) {
                TeamChatClientState.setDraft(draft.substring(0, draft.length() - 1));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            close();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        TeamChatClientState.stopBannerInput();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void sendCurrentMessage() {
        if (!TeamChatClientState.canSend()) {
            return;
        }

        String message = TeamChatClientState.getDraft().trim();
        if (message.isEmpty() || !TeamChatMessengerRenderer.fitsSingleBubble(MinecraftClient.getInstance().textRenderer, message)) {
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(message);
        ClientPlayNetworking.send(TeamChatPackets.SEND, buf);
        TeamChatClientState.setDraft("");
        close();
    }
}
