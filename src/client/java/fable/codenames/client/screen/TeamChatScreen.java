package fable.codenames.client.screen;

import fable.codenames.chat.TeamChatPackets;
import fable.codenames.client.chat.TeamChatClientState;
import fable.codenames.client.chat.TeamChatMessengerRenderer;
import fable.codenames.client.chat.TeamChatVisuals;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public class TeamChatScreen extends Screen {
    private static final int PANEL_WIDTH = TeamChatMessengerRenderer.PANEL_WIDTH;
    private static final int PANEL_HEIGHT = TeamChatMessengerRenderer.PANEL_HEIGHT;

    private final String teamName;
    private TextFieldWidget input;
    private ButtonWidget sendButton;
    private ButtonWidget clearButton;
    private int scrollOffset;

    public TeamChatScreen(String teamName) {
        super(styled("Командный чат"));
        this.teamName = teamName;
    }

    @Override
    protected void init() {
        int x = (this.width - PANEL_WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;

        this.input = new TextFieldWidget(this.textRenderer, x + 12, y + PANEL_HEIGHT - 28, PANEL_WIDTH - 88, 18, styled("Сообщение"));
        this.input.setMaxLength(32767);
        this.input.setTextPredicate(value -> value.isEmpty() || TeamChatMessengerRenderer.fitsSingleBubble(this.textRenderer, value));
        this.input.setEditable(TeamChatClientState.canSend());
        if (!TeamChatClientState.canSend()) {
            this.input.setText("Писать может только лидер");
            this.input.setEditable(false);
        }
        this.addDrawableChild(this.input);

        this.sendButton = ButtonWidget.builder(styled("Отправить"), button -> sendCurrentMessage())
                .dimensions(x + PANEL_WIDTH - 68, y + PANEL_HEIGHT - 29, 56, 20)
                .build();
        this.sendButton.active = TeamChatClientState.canSend();
        this.addDrawableChild(this.sendButton);

        this.clearButton = ButtonWidget.builder(styled("X"), button -> clearChat())
                .dimensions(x + PANEL_WIDTH - 24, y + 8, 14, 14)
                .build();
        this.addDrawableChild(this.clearButton);
        setInitialFocus(this.input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        int x = (this.width - PANEL_WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(PANEL_WIDTH / 64.0F, PANEL_HEIGHT / 128.0F, 1.0F);
        context.drawTexture(backgroundTexture(), 0, 0, 0, 0, 64, 128, 64, 128);
        context.getMatrices().pop();

        context.drawText(this.textRenderer, this.title, x + 12, y + 10, 0xFFFFFFFF, false);

        renderMessages(context, x, y);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderMessages(DrawContext context, int x, int y) {
        int chatTop = y + TeamChatMessengerRenderer.CHAT_TOP;
        int chatBottom = y + TeamChatMessengerRenderer.CHAT_BOTTOM;

        List<TeamChatMessengerRenderer.RenderedMessage> rendered = TeamChatMessengerRenderer.buildMessages(this.textRenderer, TeamChatClientState.getMessages());
        int totalHeight = TeamChatMessengerRenderer.totalHeight(rendered);
        int cursorY = chatBottom - 6 - totalHeight + this.scrollOffset;

        context.enableScissor(x + 6, chatTop, x + PANEL_WIDTH - 6, chatBottom);
        for (TeamChatMessengerRenderer.RenderedMessage message : rendered) {
            if (cursorY + message.height() < chatTop - 6) {
                cursorY += message.height();
                continue;
            }
            if (cursorY > chatBottom + 6) {
                break;
            }
            TeamChatMessengerRenderer.drawScreenBubble(context, this.textRenderer, x, cursorY, message);
            cursorY += message.height();
        }
        context.disableScissor();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            sendCurrentMessage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        this.scrollOffset += (int) (amount * 14);
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void sendCurrentMessage() {
        if (!TeamChatClientState.canSend() || this.input == null) {
            return;
        }

        String message = this.input.getText().trim();
        if (message.isEmpty() || !TeamChatMessengerRenderer.fitsSingleBubble(this.textRenderer, message)) {
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(message);
        ClientPlayNetworking.send(TeamChatPackets.SEND, buf);
        this.input.setText("");
    }

    private void clearChat() {
        ClientPlayNetworking.send(TeamChatPackets.REQUEST_CLEAR, PacketByteBufs.empty());
    }

    private Identifier backgroundTexture() {
        return TeamChatVisuals.backgroundTexture(this.teamName);
    }

    private static Text styled(String value) {
        return TeamChatMessengerRenderer.styled(value);
    }
}
