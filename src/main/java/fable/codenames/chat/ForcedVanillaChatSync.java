package fable.codenames.chat;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ForcedVanillaChatSync {
    private ForcedVanillaChatSync() {
    }

    public static void sendToAll(MinecraftServer server, Text message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeText(message);
            ServerPlayNetworking.send(player, TeamChatPackets.FORCE_VANILLA_CHAT, buf);
        }
    }
}
