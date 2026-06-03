package fable.codenames.pedestal;

import fable.codenames.board.BoardCellType;
import fable.codenames.chat.TeamChatSync;
import fable.codenames.game.CodenamesGameService;
import fable.codenames.game.CodenamesGames;
import fable.codenames.game.CodenamesPhase;
import fable.codenames.game.GameTitleService;
import fable.codenames.role.PlayerRole;
import fable.codenames.role.RoleDisplay;
import fable.codenames.role.RoleValidation;
import fable.codenames.role.Roles;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PedestalService {
    private static final int COUNTDOWN_TICKS = 60;
    private static final Map<UUID, PedestalState.Assignment> MANAGED_SELECTIONS = new HashMap<>();
    private static long countdownStartTick = -1L;
    private static int lastCountdownSecond = -1;
    private static String lastWarningKey = "";
    private static boolean hadPedestalPlayers;
    private static boolean emptyPedestalsReported;

    private PedestalService() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(PedestalService::tick);
    }

    private static void tick(MinecraftServer server) {
        PedestalState pedestalState = PedestalState.get(server);
        if (pedestalState.getAssignments().isEmpty()) {
            cancelCountdown(server, false);
            return;
        }

        if (!CodenamesGameService.isLobbyReady(server)) {
            cancelCountdown(server, false);
            return;
        }

        applyPedestalSelections(server, pedestalState);
        if (!MANAGED_SELECTIONS.isEmpty()) {
            emptyPedestalsReported = false;
            reportLeaderWarnings(server, !hadPedestalPlayers);
        }
        reportEmptyPedestals(server);
        hadPedestalPlayers = !MANAGED_SELECTIONS.isEmpty();

        if (!RoleValidation.canStart(server)) {
            cancelCountdown(server, true);
            return;
        }

        long now = server.getOverworld().getTime();
        if (countdownStartTick < 0) {
            countdownStartTick = now;
            lastCountdownSecond = -1;
        }

        long elapsed = now - countdownStartTick;
        int second = (int) (elapsed / 20L);
        int value = 3 - second;
        if (value >= 1 && value != lastCountdownSecond) {
            lastCountdownSecond = value;
            GameTitleService.showCountdown(server, value);
        }

        if (elapsed >= COUNTDOWN_TICKS) {
            cancelCountdown(server, false);
            if (CodenamesGameService.start(server)) {
                GameTitleService.showStart(server);
            }
        }
    }

    private static void applyPedestalSelections(MinecraftServer server, PedestalState pedestalState) {
        Map<UUID, PedestalState.Assignment> currentSelections = new HashMap<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            BlockPos standingOn = player.getBlockPos().down();
            PedestalState.Assignment assignment = pedestalState.get(standingOn);
            if (assignment == null || server.getScoreboard().getTeam(assignment.teamName()) == null) {
                continue;
            }

            currentSelections.put(player.getUuid(), assignment);
            PedestalState.Assignment previous = MANAGED_SELECTIONS.get(player.getUuid());
            if (assignment.role() == PlayerRole.LIDER && (previous == null || previous.role() != PlayerRole.LIDER || !previous.teamName().equals(assignment.teamName()))) {
                playLeaderPedestalSound(assignment.teamName(), player);
            }
            Team team = server.getScoreboard().getTeam(assignment.teamName());
            server.getScoreboard().addPlayerToTeam(player.getGameProfile().getName(), team);
            Roles.getState(server).setRole(player.getUuid(), assignment.role());
            RoleDisplay.refreshPlayer(player, assignment.role());

            if (previous == null || !previous.equals(assignment)) {
                showFilledVanillaXpBar(player);
            }
        }

        Set<UUID> previouslyManaged = new HashSet<>(MANAGED_SELECTIONS.keySet());
        previouslyManaged.removeAll(currentSelections.keySet());
        for (UUID uuid : previouslyManaged) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                server.getScoreboard().clearPlayerTeam(player.getGameProfile().getName());
                Roles.getState(server).clearRole(uuid);
                RoleDisplay.refreshPlayer(player, null);
                clearVanillaXpBar(player);
            }
            MANAGED_SELECTIONS.remove(uuid);
        }
        MANAGED_SELECTIONS.putAll(currentSelections);
        TeamChatSync.syncAll(server);
    }


    private static void showFilledVanillaXpBar(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(1.0F, 0, 0));
    }

    private static void clearVanillaXpBar(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new ExperienceBarUpdateS2CPacket(0.0F, 0, 0));
    }

    private static void reportLeaderWarnings(MinecraftServer server, boolean force) {
        StringBuilder key = new StringBuilder();
        List<Text> messages = new ArrayList<>();
        for (Team team : server.getScoreboard().getTeams()) {
            BoardCellType type = CodenamesGameService.expectedTypeForTeam(team.getName());
            if (type != BoardCellType.RED && type != BoardCellType.BLUE) {
                continue;
            }

            int leaders = countManagedLeaders(team.getName());
            if (leaders == 0) {
                key.append(team.getName()).append(":none;");
                messages.add(leaderWarning(type, "Выберите лидера!"));
            } else if (leaders > 1) {
                key.append(team.getName()).append(":many;");
                messages.add(leaderWarning(type, "Лидер может быть только один!"));
            }
        }

        String nextKey = key.toString();
        if (nextKey.isEmpty()) {
            lastWarningKey = "";
            return;
        }
        if (force || !nextKey.equals(lastWarningKey)) {
            sendActionBar(server, joinWarnings(messages));
            lastWarningKey = nextKey;
        }
    }

    private static void reportEmptyPedestals(MinecraftServer server) {
        if (!hadPedestalPlayers || !MANAGED_SELECTIONS.isEmpty() || emptyPedestalsReported) {
            return;
        }

        List<Text> messages = new ArrayList<>();
        for (Team team : server.getScoreboard().getTeams()) {
            BoardCellType type = CodenamesGameService.expectedTypeForTeam(team.getName());
            if (type == BoardCellType.RED || type == BoardCellType.BLUE) {
                messages.add(leaderWarning(type, "Выберите лидера!"));
            }
        }
        sendActionBar(server, joinWarnings(messages));
        emptyPedestalsReported = true;
        lastWarningKey = "";
    }

    private static int countManagedLeaders(String teamName) {
        int count = 0;
        for (PedestalState.Assignment assignment : MANAGED_SELECTIONS.values()) {
            if (assignment.teamName().equals(teamName) && assignment.role() == PlayerRole.LIDER) {
                count++;
            }
        }
        return count;
    }

    private static Text leaderWarning(BoardCellType type, String message) {
        Formatting color = type == BoardCellType.RED ? Formatting.RED : Formatting.AQUA;
        return Text.literal(message).formatted(color);
    }

    private static Text joinWarnings(List<Text> messages) {
        net.minecraft.text.MutableText result = Text.empty();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                result.append(Text.literal("  |  ").formatted(Formatting.GRAY));
            }
            result.append(messages.get(i));
        }
        return result;
    }

    private static void sendActionBar(MinecraftServer server, Text message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(message, true);
        }
    }

    private static void playLeaderPedestalSound(String teamName, ServerPlayerEntity player) {
        BoardCellType type = CodenamesGameService.expectedTypeForTeam(teamName);
        if (type == BoardCellType.RED) {
            player.getWorld().playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), net.minecraft.sound.SoundCategory.PLAYERS, 10.0F, 1.0F);
            return;
        }
        if (type == BoardCellType.BLUE) {
            player.getWorld().playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_ARROW_HIT_PLAYER, net.minecraft.sound.SoundCategory.PLAYERS, 10.0F, 1.0F);
        }
    }

    private static void cancelCountdown(MinecraftServer server, boolean showTitle) {
        if (countdownStartTick >= 0 && showTitle) {
            GameTitleService.showCancelled(server);
        }
        countdownStartTick = -1L;
        lastCountdownSecond = -1;
    }
}
