package fable.codenames.game;

import fable.codenames.Codenames;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class GameTimerSync {

    public static final Identifier CHANNEL_ID = new Identifier(Codenames.MOD_ID, "game_timer");
    private static final long TIMER_TICKS = 20L * 60L;

    private GameTimerSync() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> syncToPlayer(server, handler.player));
    }

    public static void syncToAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            syncToPlayer(server, player);
        }
    }

    public static void syncToPlayer(MinecraftServer server, ServerPlayerEntity player) {
        CodenamesGameState state = CodenamesGames.getState(server);
        long now = server.getOverworld().getTime();

        boolean gameActive = state.getPhase() != CodenamesPhase.STOPPED && state.getPhase() != CodenamesPhase.FINISHED;

        long remaining = gameActive ? Math.max(0L, state.getPhaseEndTick() - now) : 0L;
        long total = gameActive ? TIMER_TICKS : 0L;

        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeBoolean(gameActive);
        buf.writeString(state.getActiveTeam());
        buf.writeString(state.getPhase().getId());
        buf.writeLong(remaining);
        buf.writeLong(total);
        buf.writeBoolean(
                state.getPhase() == CodenamesPhase.GUESSING
                        && state.getGuessesThisTurn() > 0);

        ServerPlayNetworking.send(player, CHANNEL_ID, buf);
    }
}