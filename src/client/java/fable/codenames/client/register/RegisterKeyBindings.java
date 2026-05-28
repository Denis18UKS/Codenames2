package fable.codenames.client.register;

import fable.codenames.chat.TeamChatPackets;
import fable.codenames.item.ModItems;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public final class RegisterKeyBindings {
    private static KeyBinding openTeamChat;
    private static KeyBinding applyTeamChatConfig;

    private RegisterKeyBindings() {
    }

    public static void init() {
        openTeamChat = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.codenames.open_team_chat",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.codenames"));
        applyTeamChatConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.codenames.apply_team_chat_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.codenames"));
        ClientTickEvents.END_CLIENT_TICK.register(RegisterKeyBindings::tick);
    }

    private static void tick(MinecraftClient client) {
        while (openTeamChat.wasPressed()) {
            // The team chat is intentionally opened only by clicking the world banner.
        }
        while (applyTeamChatConfig.wasPressed()) {
            applyTeamChatConfig(client);
        }
    }

    private static void applyTeamChatConfig(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        if (!client.player.getMainHandStack().isOf(ModItems.TEAM_CHAT_CONFIGURATOR.getItem())
                && !client.player.getOffHandStack().isOf(ModItems.TEAM_CHAT_CONFIGURATOR.getItem())) {
            return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(hit.getBlockPos());
        ClientPlayNetworking.send(TeamChatPackets.APPLY_CONFIG, buf);
    }
}
