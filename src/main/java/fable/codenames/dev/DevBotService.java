package fable.codenames.dev;

import fable.codenames.role.PlayerRole;
import fable.codenames.role.RoleState;
import fable.codenames.role.Roles;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DevBotService {
    private static final String PREFIX = "bot.";

    private DevBotService() {
    }

    public static String entryName(String name) {
        return PREFIX + normalize(name);
    }

    public static UUID uuid(String entryName) {
        return UUID.nameUUIDFromBytes(("codenames:" + entryName).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isBotEntry(String entryName) {
        return entryName != null && entryName.startsWith(PREFIX);
    }

    public static String displayName(String entryName) {
        return isBotEntry(entryName) ? entryName.substring(PREFIX.length()) : entryName;
    }

    public static void addBot(MinecraftServer server, Team team, String name) {
        server.getScoreboard().addPlayerToTeam(entryName(name), team);
    }

    public static boolean removeBot(MinecraftServer server, String name) {
        String entry = entryName(name);
        boolean removed = server.getScoreboard().clearPlayerTeam(entry);
        Roles.getState(server).clearRole(uuid(entry));
        return removed;
    }

    public static void setRole(MinecraftServer server, String name, PlayerRole role) {
        RoleState state = Roles.getState(server);
        UUID botUuid = uuid(entryName(name));
        if (role == null) {
            state.clearRole(botUuid);
        } else {
            state.setRole(botUuid, role);
        }
    }

    public static PlayerRole getRole(MinecraftServer server, String entryName) {
        return Roles.getState(server).getRole(uuid(entryName));
    }

    public static List<String> listBots(MinecraftServer server) {
        List<String> bots = new ArrayList<>();
        for (Team team : server.getScoreboard().getTeams()) {
            for (String entry : team.getPlayerList()) {
                if (isBotEntry(entry)) {
                    bots.add(entry);
                }
            }
        }
        bots.sort(String::compareToIgnoreCase);
        return bots;
    }

    public static MutableText describeBots(MinecraftServer server) {
        List<String> bots = listBots(server);
        if (bots.isEmpty()) {
            return Text.literal("Тестовых ботов пока нет.").formatted(Formatting.YELLOW);
        }

        MutableText text = Text.literal("Тестовые боты: ").formatted(Formatting.GOLD);
        boolean first = true;
        for (String entry : bots) {
            Team team = server.getScoreboard().getPlayerTeam(entry);
            PlayerRole role = getRole(server, entry);
            if (!first) {
                text.append(Text.literal(", ").formatted(Formatting.GRAY));
            }
            first = false;
            text.append(Text.literal(displayName(entry)).formatted(Formatting.WHITE));
            if (team != null) {
                text.append(Text.literal("[" + team.getName() + "]").formatted(Formatting.AQUA));
            }
            if (role != null) {
                text.append(Text.literal("{" + role.getId() + "}").formatted(Formatting.GREEN));
            }
        }
        return text;
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
    }
}
