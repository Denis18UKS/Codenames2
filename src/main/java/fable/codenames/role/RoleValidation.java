package fable.codenames.role;

import fable.codenames.dev.DevBotService;
import fable.codenames.dev.SoloModeService;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

public final class RoleValidation {
    private RoleValidation() {
    }

    public static boolean validateAndBroadcast(MinecraftServer server) {
        if (SoloModeService.getState(server).getActivePlayer() != null) {
            broadcast(server, Text.literal("Включён solo-режим теста: один игрок считается лидером и угадывающим одновременно."));
            return true;
        }

        Team redTeam = findTeam(server, "red", "крас");
        Team blueTeam = findTeam(server, "blue", "син");
        if (redTeam == null || blueTeam == null) {
            broadcast(server, Text.literal("Нужны команды красных и синих: название должно содержать red/крас и blue/син."));
            return false;
        }

        int redSize = countParticipants(server, redTeam);
        int blueSize = countParticipants(server, blueTeam);

        if (!validateTeamSize(server, redSize, redTeam.getName()) | !validateTeamSize(server, blueSize, blueTeam.getName())) {
            return false;
        }

        if (redSize != blueSize) {
            broadcast(server, Text.literal("Красных и синих должно быть поровну: 2x2, 3x3, 4x4 или 5x5."));
            return false;
        }

        boolean redValid = validateTeamRoles(server, redTeam, redSize);
        boolean blueValid = validateTeamRoles(server, blueTeam, blueSize);
        return redValid && blueValid;
    }

    public static boolean canStart(MinecraftServer server) {
        if (SoloModeService.getState(server).getActivePlayer() != null) {
            return true;
        }

        Team redTeam = findTeam(server, "red", "крас");
        Team blueTeam = findTeam(server, "blue", "син");
        if (redTeam == null || blueTeam == null) {
            return false;
        }

        int redSize = countParticipants(server, redTeam);
        int blueSize = countParticipants(server, blueTeam);
        return redSize == blueSize
                && redSize >= 2
                && redSize <= 5
                && countLeaders(server, redTeam) == 1
                && countLeaders(server, blueTeam) == 1
                && countGuessers(server, redTeam) == redSize - 1
                && countGuessers(server, blueTeam) == blueSize - 1;
    }

    private static boolean validateTeamSize(MinecraftServer server, int size, String teamName) {
        if (size < 2 || size > 5) {
            broadcast(server, Text.literal("Для команды " + teamName + " нужно от 2 до 5 игроков."));
            return false;
        }
        return true;
    }

    private static boolean validateTeamRoles(MinecraftServer server, Team team, int size) {
        int leaders = countLeaders(server, team);
        if (leaders == 0) {
            broadcast(server, Text.literal("Для команды " + team.getName() + " - выберите лидера!"));
            return false;
        }

        if (leaders > 1) {
            broadcast(server, Text.literal("Для команды " + team.getName() + " - лидер может быть только один!"));
            return false;
        }

        int guessers = countGuessers(server, team);
        if (guessers != size - 1) {
            broadcast(server, Text.literal("Для команды " + team.getName() + " должно быть 1 лидер и " + (size - 1) + " угадывающих."));
            return false;
        }

        return true;
    }

    private static Team findTeam(MinecraftServer server, String englishNeedle, String russianNeedle) {
        for (Team team : server.getScoreboard().getTeams()) {
            String normalized = team.getName().toLowerCase(Locale.ROOT);
            if (normalized.contains(englishNeedle) || normalized.contains(russianNeedle)) {
                return team;
            }
        }
        return null;
    }

    private static int countParticipants(MinecraftServer server, Team team) {
        int participants = 0;
        for (String entry : team.getPlayerList()) {
            if (DevBotService.isBotEntry(entry)) {
                participants++;
                continue;
            }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry);
            if (player != null) {
                participants++;
            }
        }
        return participants;
    }

    private static int countLeaders(MinecraftServer server, Team team) {
        return countRole(server, team, PlayerRole.LIDER);
    }

    private static int countGuessers(MinecraftServer server, Team team) {
        return countRole(server, team, PlayerRole.GUESSING);
    }

    private static int countRole(MinecraftServer server, Team team, PlayerRole role) {
        RoleState state = Roles.getState(server);
        int matches = 0;
        for (String entry : team.getPlayerList()) {
            if (DevBotService.isBotEntry(entry)) {
                if (state.getRole(DevBotService.uuid(entry)) == role) {
                    matches++;
                }
                continue;
            }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry);
            if (player != null && state.getRole(player.getUuid()) == role) {
                matches++;
            }
        }
        return matches;
    }

    private static void broadcast(MinecraftServer server, Text message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(message, true);
        }
    }
}
