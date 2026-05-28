package fable.codenames.client;

import fable.codenames.chat.TeamChatPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;

public final class OsporitClientActions {
    private OsporitClientActions() {
    }

    public static void disputeClue() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        ClientPlayNetworking.send(TeamChatPackets.DISPUTE_CLUE, PacketByteBufs.empty());
    }

    public static void runTestAction() {
        disputeClue();
    }
}
