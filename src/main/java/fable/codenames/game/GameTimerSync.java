package fable.codenames.game;

import fable.codenames.Codenames;
import fable.codenames.board.TeamService;
import fable.codenames.role.PlayerRole;
import fable.codenames.role.Roles;
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

    public static void syncToPlayer(MinecraftServer server,
            ServerPlayerEntity player) {

        CodenamesGameState state = CodenamesGames.getState(server);

        long now = server.getOverworld().getTime();

        boolean timerRunning = isTimerPhase(state.getPhase())
                && state.getPhaseEndTick() > now
                && !CodenamesGameService.isPausedForMissingPlayers();

        boolean canSee = timerRunning
                && canSeeTimer(player, state);

        long remaining = canSee
                ? Math.max(0L, state.getPhaseEndTick() - now)
                : 0L;

        long total = canSee
                ? TIMER_TICKS
                : 0L;

        PacketByteBuf buf = PacketByteBufs.create();

        // ВАЖНО:
        // active = canSee
        // иначе таймер зависает у игроков НЕ активного хода
        buf.writeBoolean(canSee);

        buf.writeString(state.getActiveTeam());

        buf.writeString(state.getPhase().getId());

        buf.writeLong(remaining);

        buf.writeLong(total);

        buf.writeBoolean(
                state.getPhase() == CodenamesPhase.GUESSING
                        && state.getGuessesThisTurn() > 0);

        ServerPlayNetworking.send(player, CHANNEL_ID, buf);
    }

    private static boolean isTimerPhase(CodenamesPhase phase) {

        return phase == CodenamesPhase.GUESSING
                || phase == CodenamesPhase.WAITING_CLUE;
    }

    private static boolean canSeeTimer(ServerPlayerEntity player,
            CodenamesGameState state) {

        String teamName = TeamService.getTeamName(player);

        if (teamName == null) {
            return false;
        }

        if (!teamName.equals(state.getActiveTeam())) {
            return false;
        }

        PlayerRole role = Roles.getState(player.getServer())
                .getRole(player.getUuid());

        // ЛИДЕРЫ
        if (state.getPhase() == CodenamesPhase.WAITING_CLUE) {

            return role == PlayerRole.LIDER;
        }

        // ОТГАДЫВАЮЩИЕ
        if (state.getPhase() == CodenamesPhase.GUESSING) {

            return role == PlayerRole.GUESSING;
        }

        return false;
    }
}