package fable.codenames.score;

import fable.codenames.Codenames;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class TeamScoreSync {
    public static final Identifier CHANNEL_ID = new Identifier(Codenames.MOD_ID, "team_scores");

    private TeamScoreSync() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncToPlayer(server, handler.player));
    }

    public static void syncToAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            syncToPlayer(server, player);
        }
    }

    public static void syncToPlayer(MinecraftServer server, ServerPlayerEntity player) {
        TeamScoreState state = TeamScores.getState(server);
        PacketByteBuf buf = PacketByteBufs.create();
        Map<String, Integer> scores = state.getScores();

        buf.writeVarInt(scores.size());
        scores.forEach((teamName, score) -> {
            buf.writeString(teamName);
            buf.writeVarInt(score);
        });

        ServerPlayNetworking.send(player, CHANNEL_ID, buf);
    }
}
