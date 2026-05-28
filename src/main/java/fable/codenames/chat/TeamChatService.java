package fable.codenames.chat;

import fable.codenames.dev.SoloModeService;
import fable.codenames.game.CodenamesGameService;
import fable.codenames.role.PlayerRole;
import fable.codenames.role.Roles;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;

public final class TeamChatService {
    private TeamChatService() {
    }

    public static String getPlayerTeamName(ServerPlayerEntity player) {
        AbstractTeam team = player.getScoreboardTeam();
        return team != null ? team.getName() : null;
    }

    public static boolean canWrite(ServerPlayerEntity player, String teamName) {
        if (teamName == null || !teamName.equals(getPlayerTeamName(player))) {
            return false;
        }
        if (SoloModeService.isEnabled(player.getServer(), player.getUuid())) {
            return true;
        }
        if (Roles.getState(player.getServer()).getRole(player.getUuid()) != PlayerRole.LIDER) {
            return false;
        }
        return TeamChatRightsMode.isEnabled() || CodenamesGameService.isLeaderTeamTurn(player);
    }

    public static void appendLeaderMessage(MinecraftServer server, ServerPlayerEntity sender, String rawMessage) {
        String teamName = getPlayerTeamName(sender);
        if (teamName == null) {
            sender.sendMessage(Text.literal("Сначала вступите в команду."), false);
            return;
        }

        if (!canWrite(sender, teamName)) {
            sender.sendMessage(Text.literal("Писать в чат может только лидер. В solo-режиме писать можно одному тестовому игроку.").formatted(Formatting.YELLOW), false);
            return;
        }

        String content = rawMessage.trim();
        if (content.isEmpty()) {
            return;
        }
        boolean acceptedAsClue = CodenamesGameService.tryAcceptClue(server, sender, content);
        if (acceptedAsClue) {
            return;
        }

        TeamChatMessage message = new TeamChatMessage(
                sender.getUuid(),
                sender.getName().getString(),
                teamName,
                content,
                Instant.now().toEpochMilli());
        TeamChats.getState(server).addMessage(message);
        TeamChatSync.syncAll(server);
    }
}
